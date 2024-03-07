package com.example.nsyy.server.api;

public class ReadMessagesParam {

    public String url;
    public Integer read_type;
    public Integer cur_user_id;
    public Integer chat_user_id;
    public Integer start;
    public Integer count;

    public ReadMessagesParam() {

    }

    public ReadMessagesParam(String url, Integer cur_user_id, Integer chat_user_id, Integer read_type, Integer start, Integer count) {
        this.url = url;
        this.cur_user_id = cur_user_id;
        this.chat_user_id = chat_user_id;
        this.read_type = read_type;
        this.start = start;
        this.count = count;
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

    public Integer getRead_type() {
        return read_type;
    }

    public void setRead_type(int read_type) {
        this.read_type = read_type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "ReadChatMessageParam{" +
                "url='" + url + '\'' +
                ", cur_user_id=" + cur_user_id +
                ", chat_user_id=" + chat_user_id +
                ", read_type=" + read_type +
                ", start=" + start +
                ", count=" + count +
                '}';
    }
}
