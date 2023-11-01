package com.kamalmeet.chat;

import java.util.List;


import com.google.common.primitives.Floats;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;

public class LLMUtility {

    private String embeddingsEndpoint="https://api.openai.com/v1/embeddings";
    private String chatEndpoint="https://api.openai.com/v1/chat/completions";
    //TODO add key here
    private String key = "keyhere";

    public String getSummary(String line){
        String retstr="";
        line = "Please generate text summarization for following data -- " + line;
        HttpResponse<JsonNode> response = Unirest.post(chatEndpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer "+key)
                .body("{\n \"model\": \"gpt-3.5-turbo\", " +
                
                        "\"messages\":[{\"role\": \"user\",\"content\": \"" + line + "\"" +
                        "}]}")
                .asJson();
        if(response.getStatus() == 200){
            JsonNode responseObject = new JsonNode(response.getBody().toString());
            if(responseObject.getObject() != null){
                JSONObject obj = responseObject.getObject();
                JSONArray data = obj.getJSONArray("choices");
                JSONObject data1 = data.getJSONObject(0);
                JSONObject data2 = data1.getJSONObject("message");
                retstr = data2.getString("content");
            }
        }
        return retstr;
    }
    public List<Float> getEmbeddings(String line){

        JSONArray vectorArray = new JSONArray();
        HttpResponse<JsonNode> response = Unirest.post(embeddingsEndpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer "+key)
                .body("{\n \"model\": \"text-embedding-ada-002\", " +
                        
                        "\"input\":\"" + line + "\"" +
                        "}")
                .asJson();
        if(response.getStatus() == 200){
            JsonNode responseObject = new JsonNode(response.getBody().toString());
            if(responseObject.getObject() != null){
                JSONObject obj = responseObject.getObject();
                JSONArray data = obj.getJSONArray("data");
                JSONObject data1 = data.getJSONObject(0);
                vectorArray = data1.getJSONArray("embedding");
            }

        }

        return Floats.asList(fillData(vectorArray));
    }

    private float[] fillData(JSONArray jsonArray){

        float[] fData = new float[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                fData[i] = Float.parseFloat(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return fData;
    }
}