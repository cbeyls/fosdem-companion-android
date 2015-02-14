package be.digitalia.fosdem.model;

/**
 * Created by Abhishek on 14/02/15.
 */
public class Speaker {

    private String name;
    private String designation;
    private String linkedInURl;
    private String twitterUrl;
    private String imageUrl;
    private String information;

    public Speaker(String name, String designation, String linkedInURl, String twitterUrl, String imageUrl, String information) {
        this.name = name;
        this.designation = designation;
        this.linkedInURl = linkedInURl;
        this.twitterUrl = twitterUrl;
        this.imageUrl = imageUrl;
        this.information = information;
    }

    public String getName() {
        return name;
    }

    public String getDesignation() {
        return designation;
    }

    public String getLinkedInURl() {
        return linkedInURl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getInformation() {
        return information;
    }
}
