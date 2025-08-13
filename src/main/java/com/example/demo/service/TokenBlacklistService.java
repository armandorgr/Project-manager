package com.example.demo.service;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {
    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION = "blacklisted_tokens";

    public TokenBlacklistService(MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }

    public void blacklistToken(String token, long ttlMillis){
        Document tokenDoc = new Document("_id", token)
                .append("expiry", Instant.now().plusMillis(ttlMillis));

        mongoTemplate.insert(tokenDoc, COLLECTION);

        createTTLIndex();
    }

    public boolean isTokenBlackListed(String token){
        return mongoTemplate.exists(
                Query.query(Criteria.where("_id").is(token)),
                COLLECTION
        );
    }

    private void createTTLIndex(){
        if(mongoTemplate.indexOps(COLLECTION).getIndexInfo().stream().noneMatch(index -> "expiry_ttl_index".equals(index.getName()))){
            IndexOperations indexOps = mongoTemplate.indexOps(COLLECTION);
            IndexDefinition index = new Index()
                    .named("expiry_ttl_index")
                    .on("expiry", Sort.Direction.ASC)
                    .expire(Duration.ofSeconds(0));
            indexOps.createIndex(index);
        }
    }
}
