package com.example.nsyy.server.controller;

import static com.example.nsyy.server.api.ReturnData.ERROR.UNKNOWN;

import com.example.nsyy.message.FileHelper;
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
    public ReturnData readChatMessage(@RequestBody ReadMessagesParam readMessagesParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.readChatMessage: 接收到请求参数： url: " + readMessagesParam.url +
                    " , cur_user_id: " + readMessagesParam.getCur_user_id() +
                    " , chat_user_id: " + readMessagesParam.getChat_user_id() +
                    " , read_type: " + readMessagesParam.getRead_type() +
                    " , start: " + readMessagesParam.getStart() +
                    " , count: " + readMessagesParam.getCount());

            if (readMessagesParam.getUrl() == null || readMessagesParam.getCur_user_id() == null) {
                returnData.setCode(UNKNOWN);
                returnData.setSuccess(false);
                returnData.setErrorMsg("Failed to read chat message, params is " + readMessagesParam.toString());
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
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(0, dict);
            }else if (readMessagesParam.getRead_type() == 1) {
                // 私聊
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(1, dict);
            } else if (readMessagesParam.getRead_type() == 2) {
                // 群聊
                messages = FileHelper.getInstance().updateLocalDataAndReturnMsg(2, dict);
            }

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

            Map<String, String> dict = new HashMap<>();
            dict.put("url", writeMessageParam.getUrl());

            if (writeMessageParam.getChat_type() == 0) {
                // 通知消息
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(0, dict);
            } else if (writeMessageParam.getChat_type() == 1) {
                // 私聊
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(1, dict);
            } else if (writeMessageParam.getChat_type() == 2) {
                // 群聊
                dict.put("read_type", writeMessageParam.getChat_type().toString());
                dict.put("cur_user_id", writeMessageParam.getCur_user_id().toString());
                dict.put("chat_user_id", writeMessageParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(2, dict);
            }

            FileHelper.getInstance().writeMessageToLocal(writeMessageParam.getPrev_msg_id(),
                    writeMessageParam.getCur_user_id(), writeMessageParam.getMsg());

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



    /**
     * 更新本地消息
     * @param updateParam
     * @return
     */
    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/update_local_message")
    public ReturnData updateLocalMessage(@RequestBody UpdateLocalMessageParam updateParam) {
        ReturnData returnData = new ReturnData();
        try {
            System.out.println("MessageController.updateLocalMessage: 接收到请求参数： " +
                    updateParam.toString());

            if (updateParam.getUrl() == null || updateParam.getCur_user_id() == null) {
                returnData.setCode(UNKNOWN);
                returnData.setSuccess(false);
                returnData.setErrorMsg("Failed to update local message, params is " + updateParam.toString());
                return returnData;
            }

            Map<String, String> dict = new HashMap<>();
            dict.put("url", updateParam.getUrl());

            if (updateParam.getChat_type() == 0) {
                // 通知消息
                dict.put("read_type", updateParam.getChat_type().toString());
                dict.put("cur_user_id", updateParam.getCur_user_id().toString());
                dict.put("chat_user_id", updateParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(0, dict);
            } else if (updateParam.getChat_type() == 1) {
                // 私聊
                dict.put("read_type", updateParam.getChat_type().toString());
                dict.put("cur_user_id", updateParam.getCur_user_id().toString());
                dict.put("chat_user_id", updateParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(1, dict);
            } else if (updateParam.getChat_type() == 2) {
                // 群聊
                dict.put("read_type", updateParam.getChat_type().toString());
                dict.put("cur_user_id", updateParam.getCur_user_id().toString());
                dict.put("chat_user_id", updateParam.getChat_user_id().toString());
                FileHelper.getInstance().updateLocalData(2, dict);
            }

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
    public ReturnData query_file(@RequestParam("file_name") String fileName) {

        // Reading from a file
        List<String> linesRead = FileHelper.getInstance().readLinesFromFile(fileName, FileHelper.ALL_LINE);
        System.out.println("===== query " + fileName + " =====");
        for (String line : linesRead) {
            System.out.println("Message: " + line);
        }

        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData(linesRead);
        return returnData;
    }

    @CrossOrigin(methods = {RequestMethod.DELETE})
    @DeleteMapping(path = "/delete_file")
    public ReturnData delete_file(@RequestParam("file_name") String fileName) {
        System.out.println("===== delete " + fileName + " =====");
        FileHelper.getInstance().deleteFile(fileName);

        ReturnData returnData = new ReturnData();
        returnData.setSuccess(true);
        returnData.setCode(200);
        returnData.setData("delete ok");
        return returnData;
    }
}
