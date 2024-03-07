package com.example.nsyy.message;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileHelper {

    // 读取所有行数据
    public static int ALL_LINE = -1;
    // 读取最后一行数据
    public static int LAST_LINE = 1;

    private volatile static FileHelper uniqueInstance;
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    //采用Double CheckLock(DCL)实现单例
    public static FileHelper getInstance() {
        if (uniqueInstance == null) {
            synchronized (FileHelper.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FileHelper();
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * 读取消息
     * 1. 先从服务器加载最新消息，写入本地文件
     * 2. 从本地文件中查找指定消息
     *
     * @param type 0-通知消息 1-私聊 2-群聊
     * @param dict
     * @return
     */
    public List<Map<String, Object>> updateLocalDataAndReturnMsg(int type, Map<String, String> dict) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 根据文件中是否存在消息，更新消息
        String fileName = "";
        if (type == 0) {
            // 通知消息
            String cur_user_id = dict.get("cur_user_id");
            fileName = "nsyy_notification_message_" + cur_user_id;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);

            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", cur_user_id);
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", cur_user_id);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }
        } else if (type == 1) {
            // 私聊消息
            String sender = dict.get("cur_user_id");
            String receiver = dict.get("chat_user_id");

            if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                fileName =  "nsyy_private_message_" + sender + "_" + receiver;
            } else {
                fileName =  "nsyy_private_message_" + receiver + "_" + sender;
            }

            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", sender);
                    params.put("chat_user_id", receiver);
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", sender);
                params.put("chat_user_id", receiver);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }


        } else if (type == 2) {
            // 群聊消息
            String cur_user_id = dict.get("cur_user_id");
            String chat_user_id = dict.get("chat_user_id");

            fileName =  "nsyy_group_message_" + chat_user_id;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", cur_user_id);
                    params.put("chat_user_id", chat_user_id);
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", cur_user_id);
                params.put("chat_user_id", chat_user_id);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }
        }

        // 从文件中读取消息
        List<String> messageInFile = readLinesFromFile(fileName, ALL_LINE);
        int start = Integer.parseInt(dict.get("start"));
        int count = Integer.parseInt(dict.get("count"));
        for (String msg: messageInFile) {
            try {
                // Convert JSON string to JSON object
                JSONObject jsonObject = new JSONObject(msg);
                String idStr = jsonObject.getString("id");
                int id = Integer.parseInt(idStr);
                if (id >= start) {
                    JSONObject object = new JSONObject(msg);
                    Map<String, Object> jsonmsg = jsonToMap(object);
                    if (0 == ((Number) jsonmsg.get("chat_type")).intValue()) {
                        jsonmsg.put("context", jsonToMap(new JSONObject(((String) jsonmsg.get("context")))));
                    }
                    messages.add(jsonmsg);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (messages.size() >= count){
                break;
            }
        }

        return messages;
    }

    public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        // Iterate over the keys in the JSONObject
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            // If the value is another JSONObject, recursively convert it to a Map
            if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
            }

            // Put the key-value pair into the map
            map.put(key, value);
        }

        return map;
    }


    public void writeMessageToLocal(int prevMsgId, int userId, String msg) {
        JSONObject newMessage = null;
        String fileName = "";

        List<String> writemsg = new ArrayList<>();
        writemsg.add(msg);
        try {
            // Convert JSON string to JSON object
            newMessage = new JSONObject(msg);
            int type = Integer.parseInt(newMessage.getString("chat_type"));

            if (type == 0) {
                // 通知类型
                String receiver = newMessage.getString("receiver");
                if (receiver.contains(Integer.toString(userId))) {
                    fileName = "nsyy_notification_message_" + Integer.toString(userId);
                    List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
                    if (lastMsg.isEmpty()) {
                        // 文件之前不存在，直接存入
                        writeLinesToFile(writemsg, fileName);
                    } else {
                        // Convert JSON string to JSON object
                        JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                        int id = jsonObject.getInt("id");

                        if (id == prevMsgId) {
                            writeLinesToFile(writemsg, fileName);
                        }
                    }
                }
            } else if (type == 1) {
                // 私聊
                String sender = newMessage.getString("sender");
                String receiver = newMessage.getString("receiver");
                if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                    fileName = "nsyy_private_message_" + sender + "_" + receiver;
                } else {
                    fileName = "nsyy_private_message_" + receiver + "_" + sender;
                }
                List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
                if (lastMsg.isEmpty()) {
                    // 文件之前不存在，直接存入
                    writeLinesToFile(writemsg, fileName);
                } else {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    int id = jsonObject.getInt("id");

                    if (id == prevMsgId) {
                        writeLinesToFile(writemsg, fileName);
                    }
                }
            } else if (type == 2) {
                // 群聊
                String groupId = newMessage.getString("group_id");
                fileName = "nsyy_group_message_" + groupId;
                List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
                if (lastMsg.isEmpty()) {
                    // 文件之前不存在，直接存入
                    writeLinesToFile(writemsg, fileName);
                } else {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    int id = jsonObject.getInt("id");

                    if (id == prevMsgId) {
                        writeLinesToFile(writemsg, fileName);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从服务器加载离线消息，更新本地消息
     * @param type
     * @param dict
     */
    public void updateLocalData(int type, Map<String, String> dict) {
        // 根据文件中是否存在消息，更新消息
        String fileName = "";
        if (type == 0) {
            // 通知消息
            String cur_user_id = dict.get("cur_user_id");
            fileName = "nsyy_notification_message_" + cur_user_id;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);

            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", dict.get("cur_user_id"));
                    params.put("chat_user_id", dict.get("chat_user_id"));
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", dict.get("cur_user_id"));
                params.put("chat_user_id", dict.get("chat_user_id"));
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }
        } else if (type == 1) {
            // 私聊消息
            String sender = dict.get("cur_user_id");
            String receiver = dict.get("chat_user_id");

            if (Integer.parseInt(sender) <= Integer.parseInt(receiver)) {
                fileName =  "nsyy_private_message_" + sender + "_" + receiver;
            } else {
                fileName =  "nsyy_private_message_" + receiver + "_" + sender;
            }

            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", sender);
                    params.put("chat_user_id", receiver);
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", sender);
                params.put("chat_user_id", receiver);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }


        } else if (type == 2) {
            // 群聊消息
            String sender = dict.get("cur_user_id");
            String receiver = dict.get("chat_user_id");

            fileName =  "nsyy_group_message_" + receiver;
            List<String> lastMsg = readLinesFromFile(fileName, LAST_LINE);
            if (!lastMsg.isEmpty()) {
                // 文件存在消息，根据最后一条消息进行更新
                try {
                    // Convert JSON string to JSON object
                    JSONObject jsonObject = new JSONObject(lastMsg.get(0));
                    String id = jsonObject.getString("id");

                    Map<String, String> params = new HashMap<>();
                    params.put("read_type", dict.get("read_type"));
                    params.put("cur_user_id", sender);
                    params.put("chat_user_id", receiver);
                    params.put("start", Integer.toString(Integer.parseInt(id) + 1));
                    params.put("count", Integer.toString(500));

                    synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);

                } catch (JSONException e) {
                    System.out.println("===> FileHelper.updateLocalDataAndReturnMsg error");
                    e.printStackTrace();
                }
            } else {
                // 文件不存在消息，直接从数据库查询
                Map<String, String> params = new HashMap<>();
                params.put("read_type", dict.get("read_type"));
                params.put("cur_user_id", sender);
                params.put("chat_user_id", receiver);
                params.put("start", Integer.toString(-1));
                params.put("count", Integer.toString(500));

                synchronousHttpPostRequest(dict.get("url"), dictToJsonStr(params), fileName);
            }
        }

    }


    public void synchronousHttpPostRequest(String urlString, String jsonInputString, String fileName) {
        try {
            // Specify the URL for the HTTP POST request
            URL url = new URL(urlString);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Enable input and output streams
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Set the request headers (optional)
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
//
//            // Create the JSON payload for the POST request
//            String jsonInputString = "{\"key1\": \"value1\", \"key2\": \"value2\"}";

            // Write the JSON payload to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response from the server
            String responseData = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseData = response.toString();
                System.out.println("Response: " + responseData);
            }

            // Convert JSON string to JSON object
            JSONObject jsonObject = new JSONObject(responseData);
            int code = jsonObject.getInt("code");
            // 查询成功，将查询到的消息写入文件
            if (code == 20000) {
                JSONArray msgs = jsonObject.getJSONArray("data");
                writeLinesToFile(jsonArrayToList(msgs), fileName);
            }

            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 将 JSONArray 转换为 List<String>
     * @param jsonArray
     * @return
     */
    private static List<String> jsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();

        try {
            // Iterate through the JSONArray and add each item to the list
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    public String dictToJsonStr(Map<String, String> dict) {
        String jsonString = "";
        try {
            // Create a JSON object
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : dict.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }

            // Convert JSON object to JSON string
            jsonString = jsonObject.toString();

            // Use the resulting JSON string as needed
            System.out.println("JSON String: " + jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }


    // 按行写入文件
    private void writeLinesToFile(List<String> lines, String fileName) {
        // Get the file path for internal storage
        String filePath = context.getFilesDir() + fileName;
        System.out.println("准备追加 " + lines.size() + " 条消息到文件 " + filePath);
        if (lines.isEmpty()) {
            return ;
        }

        try (FileOutputStream fos = new FileOutputStream(filePath, true);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos))) {

            for (String line : lines) {
                writer.write(line);
                writer.newLine();  // Add a newline character after each line
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // 按行读取文件数据
    // -1 读取所有数据
    // 1 读取最后一行的消息
    public List<String> readLinesFromFile(String fileName, int count) {
        // Get the file path for internal storage
        String filePath = context.getFilesDir() + fileName;
        List<String> allLines = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            // Read all line data
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }

            int size = allLines.size();
            if (count == LAST_LINE && size > 0){
                String lastLine = allLines.get(size - 1);
                allLines.clear();
                allLines.add(lastLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allLines;
    }

    public void deleteFile(String fileName) {
        try {
            // Get the file path for internal storage
            String filePath = context.getFilesDir() + fileName;
            // Create a File object with the specified file path
            File fileToDelete = new File(filePath);

            // Check if the file exists before attempting to delete
            if (fileToDelete.exists()) {
                // Attempt to delete the file
                if (fileToDelete.delete()) {
                    System.out.println("File deleted successfully.");
                } else {
                    System.err.println("Unable to delete the file.");
                }
            } else {
                System.err.println("File does not exist.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
