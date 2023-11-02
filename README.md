# chatbot
The sample application helps setup a basic version of Chatbot. 

There are three APIs implemented

/insertdata: This helps user insert the data into the Milvus database

/searchdata: This takes a query and searches the data in Milvus database 

/printdata: This prints the details of the data in Milvus database


**Code Details**


ChatBotApplication.java: SpringBoot Application with main method

ChatBotController.java: Controller layer supporting the three APIs mentioned above

LLMUtility.java: Manage the connectivity with LLM for generating Embeddings and Summarization of the text

MilvusUtility.java: Methods to interact and manage data in Milvus Database. insertdata methods read from a file and add to the milvus database. searchdata method implements search feature. 

**Run the code**

This is a SpringBoot Application. One can execute by executing ChatBotApplication. 

