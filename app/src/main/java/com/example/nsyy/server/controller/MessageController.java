package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.ReturnData.ERROR.UNKNOWN;

import com.example.nsyy.message.FileHelper;
import com.example.nsyy.server.api.GroupContactParam;
import com.example.nsyy.server.api.ReadChatsParam;
import com.example.nsyy.server.api.ReadMessagesParam;
import com.example.nsyy.server.api.UpdateLocalMessageParam;
import com.example.nsyy.server.api.ReturnData;
import com.example.nsyy.server.api.WriteMessageParam;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.DeleteMapping;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    /**
     * 读取聊天消息
     * @param readMessagesParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/read_messages")
    public ReturnData readMessages(@RequestBody ReadMessagesParam readMessagesParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.readMessages: 接收到请求参数: " + readMessagesParam.toString());

            if (readMessagesParam.getUrl() == null || readMessagesParam.getCur_user_id() == null) {
                returnData.setCode(UNKNOWN);
                returnData.setSuccess(false);
                returnData.setErrorMsg("Failed to read chat message, params is "
                        + readMessagesParam);
                return returnData;
            }

            Map<String, String> dict = new HashMap<>();
            dict.put("url", readMessagesParam.getUrl());
            dict.put("cur_user_id", Integer.toString(readMessagesParam.getCur_user_id()));
            dict.put("read_type", Integer.toString(readMessagesParam.getRead_type()));

            if (readMessagesParam.getChat_user_id() != null) {
                dict.put("chat_user_id", Integer.toString(readMessagesParam.getChat_user_id()));
            }
            dict.put("start", Integer.toString(readMessagesParam.getStart()));
            dict.put("count", Integer.toString(readMessagesParam.getCount()));

            List<Map<String, Object>> messages = null;
            if (readMessagesParam.getRead_type() == 0) {
                // 通知
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(0, readMessagesParam.getCur_user_id(), dict);
            }else if (readMessagesParam.getRead_type() == 1) {
                // 私聊
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(1, readMessagesParam.getCur_user_id(), dict);
            } else if (readMessagesParam.getRead_type() == 2) {
                // 群聊
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(2, readMessagesParam.getCur_user_id(), dict);
            }

            // 更新未读状态
            FileHelper.getInstance().updateUnread(readMessagesParam.getRead_type(), readMessagesParam.getCur_user_id(), readMessagesParam.getChat_user_id());

            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(messages);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to read notification message, params is " + readMessagesParam.toString());
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/read_chats")
    public ReturnData readChats(@RequestBody ReadChatsParam readChatsParam) {
        System.out.println("MessageController.readChats: 接收到请求参数: " + readChatsParam.toString());

        List<Map<String, Object>> result = new ArrayList<>();
        ReturnData returnData = new ReturnData();
        try {
            int allUnread = FileHelper.getInstance().getLocalContact(readChatsParam.getUser_id(), result);

            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData(result);
            returnData.setAll_unread(allUnread);
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to get contact list, params is " + readChatsParam.toString());
            return returnData;
        }
    }

    /**
     * 写入消息
     * @param writeMessageParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/write_message")
    public ReturnData writeMessage(@RequestBody WriteMessageParam writeMessageParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.writeMessage: 接收到请求参数： " +
                    writeMessageParam.toString());

            if (writeMessageParam.getPrev_msg_id() == null || writeMessageParam.getMsg() == null) {
                returnData.setCode(UNKNOWN);
                returnData.setSuccess(false);
                returnData.setErrorMsg("Failed to write message, params is " + writeMessageParam.toString());
                return returnData;
            }

            // 程序进入后台时，调用系统通知功能
            FileHelper.getInstance().sendNotification(writeMessageParam.getChat_type(),
                    writeMessageParam.getChat_user_name(), writeMessageParam.getMsg());

            // 写之前还是需要更新一下，防止本人发送的消息丢失
            Map<String, String> dict = new HashMap<>();
            dict.put("url", writeMessageParam.getUrl());
            if (writeMessageParam.getChat_type() == 0) {
                // 通知消息
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalDataByServer(0, dict, false);
            } else if (writeMessageParam.getChat_type() == 1) {
                // 私聊
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalDataByServer(1, dict, false);
            } else if (writeMessageParam.getChat_type() == 2) {
                // 群聊
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalDataByServer(2, dict, false);
            }

            // 将消息写入本地
            FileHelper.getInstance().writeMessageToLocal(writeMessageParam.getPrev_msg_id(),
                    writeMessageParam.getCur_user_id(), writeMessageParam.getMsg());

            // 维护联系人，未读状态
            FileHelper.getInstance().updateLocalContact(true, writeMessageParam.getChat_type(),
                    writeMessageParam.getCur_user_id(), writeMessageParam.getChat_user_id(),
                    writeMessageParam.getChat_user_name(), writeMessageParam.getIn_chat(), writeMessageParam.getMsg());


            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData("write successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to write message, params is " + writeMessageParam.toString());
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/update_group_contact")
    public ReturnData updateGroupContact(@RequestBody GroupContactParam param) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.updateGroupContact: 接收到请求参数： " +
                    param.toString());
            FileHelper.getInstance().updateLocalContact(true, 2, param.getCur_user_id(),
                    param.getGroup_id(), param.getGroup_name(), 1, null);

            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData("update successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to update local message, params is " + param.toString());
            return returnData;
        }
    }



    /**
     * 更新本地消息
     * @param updateParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/update_local_data")
    public ReturnData updateLocalData(@RequestBody UpdateLocalMessageParam updateParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.updateLocalMessage: 接收到请求参数： " +
                    updateParam.toString());

            FileHelper.getInstance().synchronousUpdateContactList(updateParam.update_chat_list_url, updateParam.cur_user_id);

            FileHelper.getInstance().asynchronousUpdateMessage(updateParam.update_msg_url, updateParam.cur_user_id);

            returnData.setSuccess(true);
            returnData.setCode(200);
            returnData.setData("update successful");
            return returnData;
        } catch (Exception e) {
            returnData.setCode(UNKNOWN);
            returnData.setSuccess(false);
            returnData.setErrorMsg("Failed to update local message, params is " + updateParam.toString());
            return returnData;
        }
    }



    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping(path = "/query_file")
    public ReturnData query_file(@RequestParam("file_name") String fileName,
                                 @RequestParam("cur_user_id") Integer curUserId,
                                 @RequestParam("type") Integer type) throws JSONException {

        // Reading from a file
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else {
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        }
        List<String> linesRead = FileHelper.getInstance().readLinesFromFile(fileName,
                FileHelper.ALL_LINE, dir);
        System.out.println("===== query " + dir + fileName + " =====");
        List<JSONObject> res = new ArrayList<>();
        for (String line : linesRead) {
            System.out.println("Message: " + line);
            res.add(new JSONObject(line));
        }


        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData(linesRead);
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping(path = "/query_file_list")
    public ReturnData query_file_list(@RequestParam("cur_user_id") Integer curUserId,
                                 @RequestParam("type") Integer type) {

        // Reading from a file
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else if (type == 1){
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        } else if (type == 2) {
            dir = "/" + FileHelper.ATTACHMENTS_DIR + "/";
        }

        List<String> fileList = FileHelper.getInstance().getFileList(dir);
        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData(fileList);
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/delete_file")
    public ReturnData delete_file(@RequestParam("file_name") String fileName,
                                  @RequestParam("cur_user_id") Integer curUserId,
                                  @RequestParam("type") Integer type) {
        String dir = "";
        if (type == 0) {
            dir = "/" + FileHelper.MESSAGES_DIR + "/" + curUserId.toString() + "/";
        } else {
            dir = "/" + FileHelper.CONTACTS_DIR + "/" + curUserId.toString() + "/";
        }
        System.out.println("===== delete " + dir + fileName + " =====");
        FileHelper.getInstance().deleteFile(fileName, dir);

        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData("delete ok");
        return returnData;
    }
}
