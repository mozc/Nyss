package com.example.nsyy.server.api;

public class WriteMessageParam {

    public String url;
    public Integer prev_msg_id;
    public Integer chat_type;
    public Integer cur_user_id;
    public Integer chat_user_id;
    public String msg;

    public WriteMessageParam() {

    }

    public WriteMessageParam(String url, Integer prev_msg_id, Integer chat_type, Integer cur_user_id, Integer chat_user_id, String msg) {
        this.url = url;
        this.prev_msg_id = prev_msg_id;
        this.chat_type = chat_type;
        this.cur_user_id = cur_user_id;
        this.chat_user_id = chat_user_id;
        this.msg = msg;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPrev_msg_id() {
        return prev_msg_id;
    }

    public void setPrev_msg_id(Integer prev_msg_id) {
        this.prev_msg_id = prev_msg_id;
    }

    public Integer getChat_type() {
        return chat_type;
    }

    public void setChat_type(Integer chat_type) {
        this.chat_type = chat_type;
    }

    public Integer getCur_user_id() {
        return cur_user_id;
    }

    public void setCur_user_id(Integer cur_user_id) {
        this.cur_user_id = cur_user_id;
    }

    public Integer getChat_user_id() {
        return chat_user_id;
    }

    public void setChat_user_id(Integer chat_user_id) {
        this.chat_user_id = chat_user_id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "WriteMessageParam{" +
                "url='" + url + '\'' +
                ", prev_msg_id=" + prev_msg_id +
                ", chat_type=" + chat_type +
                ", cur_user_id=" + cur_user_id +
                ", chat_user_id=" + chat_user_id +
                ", msg='" + msg + '\'' +
                '}';
    }
}
