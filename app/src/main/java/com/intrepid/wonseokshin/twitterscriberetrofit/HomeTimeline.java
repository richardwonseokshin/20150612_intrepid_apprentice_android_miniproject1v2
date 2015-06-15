
package com.intrepid.wonseokshin.twitterscriberetrofit;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class HomeTimeline {

    @SerializedName("created_at")
    @Expose
    private String createdAt;

    @SerializedName("text")
    @Expose
    private String text;



    /**
     * 
     * @return
     *     The createdAt
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     *
     * @param createdAt
     *     The created_at
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }



    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }


}
