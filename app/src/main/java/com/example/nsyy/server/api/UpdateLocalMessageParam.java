package com.example.nsyy.server.api;

public class UpdateLocalMessageParam {

    public String url;
    public Integer cur_user_id;
    public Integer chat_user_id;
    public Integer chat_type;

    public UpdateLocalMessageParam() {

    }

    public UpdateLocalMessageParam(String url, Integer cur_user_id, Integer chat_user_id, Integer group_id) {
        this.url = url;
        this.cur_user_id = cur_user_id;
        this.chat_user_id = chat_user_id;
        this.chat_type = chat_type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCur_user_id() {
        return cur_user_id;
    }

    public void setCur_user_id(int cur_user_id) {
        this.cur_user_id = cur_user_id;
    }

    public Integer getChat_user_id() {
        return chat_user_id;
    }

    public void setChat_user_id(int chat_user_id) {
        this.chat_user_id = chat_user_id;
    }

    public Integer getChat_type() {
        return chat_type;
    }

    public void setChat_type(int chat_type) {
        this.chat_type = chat_type;
    }

    @Override
    public String toString() {
        return "ReadChatMessageParam{" +
                "url='" + url + '\'' +
                ", cur_user_id=" + cur_user_id +
                ", chat_user_id=" + chat_user_id +
                ", chat_type=" + chat_type +
                '}';
    }
}
