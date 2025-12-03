package com.redrak.app;

public class AppStore {
    private static AppStore instance;
    private String token;
    private String profileJson;
    private AppStore(){}
    public static synchronized AppStore getInstance(){
        if(instance==null) instance=new AppStore();
        return instance;
    }
    public void setToken(String t){ token=t; }
    public String getToken(){ return token; }
    public void setProfile(String p){ profileJson=p; }
    public String getProfile(){ return profileJson; }
    public void clear(){ token=null; profileJson=null; }
    public String dumpDummy(){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<500;i++) sb.append(".").append(i);
        return sb.toString();
    }
}