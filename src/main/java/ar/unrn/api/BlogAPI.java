package ar.unrn.api;

import static com.mongodb.client.model.Accumulators.addToSet;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static spark.Spark.get;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonObject;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import com.mongodb.client.AggregateIterable;
import spark.Spark;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlogAPI {

    public static void main(String[] args) {

        Spark.init();

        //localhost:4567/

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("Blog");

        get("/pages/:id", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            //Recupero el id que viene por parámetro
            MongoCollection<Document> paginasCollection = database.getCollection("paginas");

            String paginaId = req.params("id");

            ObjectId objectId = new ObjectId(paginaId);

            Document pagina = paginasCollection.find(eq("_id", objectId)).first();

            if (pagina != null) {
                List<String> pages = new ArrayList<>();
                pages.add(pagina.toJson());
                return pages;
            } else {
                return "{ \"message\": \"Página con ID " + paginaId + " no encontrada\" }";
            }
        });

        get("/byautor", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");

            MongoCollection<Document> collection = database.getCollection("posts");

            AggregateIterable<Document> result = collection.aggregate(
                    Arrays.asList(
                            Aggregates.group("$author", Accumulators.sum("count", 1))
                    )
            );

            List<String> resultList = new ArrayList<>();
            for (Document doc : result) {
                Document authorCount = new Document();
                authorCount.put("_id", doc.get("_id"));
                authorCount.put("count", doc.get("count"));
                resultList.add(authorCount.toJson());
            }

            return resultList;

        });

        get("/ultimos4posts", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");

            MongoCollection<Document> collection = database.getCollection("posts");

            Bson sort = Sorts.descending("date");

            FindIterable<Document> result = collection.find().sort(sort).limit(4);

            List<String> posts = new ArrayList<>();

            for (Document doc : result) {
                posts.add(doc.toJson());
            }

            return posts;
        });

        get("/post/autor/:nombreautor", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");

            //Recupero el nombre del autor que viene como parámetro
            String nombreAutor = req.params("nombreautor");

            MongoCollection<Document> postCollection = database.getCollection("posts");

            Document post = postCollection.find(eq("author", nombreAutor)).first();

            if (post != null) {
                List<String> postList = new ArrayList<>();
                postList.add(post.toJson());
                return postList;
            } else {
                return "{ \"message\": \"Post con ID " + nombreAutor + " no encontrado\" }";
            }
        });

        get("/post/:id", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");


            MongoCollection<Document> postCollection = database.getCollection("posts");

            //Recupero el id del post que viene por parámetro
            String postId = req.params("id");

            ObjectId objectId = new ObjectId(postId);

            Document post = postCollection.find(eq("_id", objectId)).first();

            if (post != null) {
                List<String> posts = new ArrayList<>();
                posts.add(post.toJson());
                return posts;
            } else {
                return "{ \"message\": \"Post con ID " + postId + " no encontrado\" }";
            }
        });

        get("/search/:text", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");

            MongoCollection<Document> collection = database.getCollection("posts");

            collection.createIndex(Indexes.text("text"));

            //Recupero la palabra/frase ingresada por el usuario
            String text = req.params("text");

            FindIterable<Document> result = collection.find(Filters.text(text));

            List<String> resultList = new ArrayList<>();

            for (Document document : result) {
                resultList.add(document.toJson());
            }

            return resultList;
        });

        Spark.exception(Exception.class, (exception, request, response) ->
        {
            exception.printStackTrace();
        });
    }
}
