package hk.edu.polyu.intercloud.util;

public class StringUtil {
    private String token;
    private Integer position;
    private String stringToBeInserted;

    public StringUtil(String token, Integer position, String stringToBeInserted) {
        this.token = token;
        this.position = position;
        this.stringToBeInserted = stringToBeInserted;
    }

    public String getToken() {
        return this.token;
    }

    public Integer getPosition() {
        return this.position;
    }

    public String getStringToBeInserted() {
        return this.stringToBeInserted;
    }
}
