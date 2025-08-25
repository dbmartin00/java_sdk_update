package io.harness.fme.demo.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisplayConfig {
    @JsonProperty("call_to_action")
    private String text;

    @JsonProperty("image_src")
    private String image;

    public DisplayConfig(String text, String image) {
        this.text = text;
        this.image = image;
    }

    public String
    getText() { return this.text; }

    public void
    setText(String text) { this.text = text; }

    public String
    getImage() { return this.image; }

    public void
    setImage(String image) { this.image = image; }
}

