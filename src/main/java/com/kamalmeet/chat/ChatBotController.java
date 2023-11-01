package com.kamalmeet.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.milvus.client.MilvusServiceClient;

@RestController
@RequestMapping("/chat")
public class ChatBotController {


    /**
     * Insert data into the collection.
     *
     * @return A string indicating the status of the data insertion.
     */
    @PostMapping(value = "/insertdata")
    private String insertData() {
        MilvusUtility milvusUtility = new MilvusUtility();
        MilvusServiceClient milvusClient = milvusUtility.getClient();
        boolean createCollection = milvusUtility.createCollection(milvusClient);
        if (createCollection) {
            milvusUtility.insertData(milvusClient);
        }
        milvusUtility.releaseCollection(milvusClient);
        return "Success";
    }

    @GetMapping(value = "/searchdata")
    private String searchData(@RequestParam("string") String query) {
        LLMUtility llmUtility = new LLMUtility();
        MilvusUtility milvusUtility = new MilvusUtility();
        MilvusServiceClient milvusClient = milvusUtility.getClient();
        String result = milvusUtility.searchData(milvusClient, query);
        var summary = llmUtility.getSummary(result);
        milvusUtility.releaseCollection(milvusClient);
        return summary;
    }

    @GetMapping(value = "/printdata")
    private String printData() {
        MilvusUtility milvusUtility = new MilvusUtility();
        MilvusServiceClient milvusClient = milvusUtility.getClient();
        milvusUtility.printData(milvusClient);
        milvusUtility.releaseCollection(milvusClient);
        return "Please check logs for Data";
    }
}
