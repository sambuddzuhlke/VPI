package VPI.VertecClasses.VertecOrganisations;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * THIS CLASS IS RECEIVED FROM VERTEC
 * Dates are represented in the vertec format /Date separated by a 'T' from day-time
 */
public class Organisation {

    private Long vertecId;
    private String ownedOnVertecBy;
    private Boolean active;

    private Long ownerId;

    private String name;
    private String website;
    private String category;
    private String businessDomain;
    private String buildingName;
    private String street_no;
    private String street;
    private String city;
    private String country;
    private String zip;

    private Long parentOrganisation;

    private String modified;
    private String created;


    public Organisation(){
    }


    public Long getVertecId() {
        return vertecId;
    }

    public void setVertecId(Long vertecId) {
        this.vertecId = vertecId;
    }

    public String getOwnedOnVertecBy() {
        return ownedOnVertecBy;
    }

    public void setOwnedOnVertecBy(String ownedOnVertecBy) {
        this.ownedOnVertecBy = ownedOnVertecBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getStreet_no() {
        return street_no;
    }

    public void setStreet_no(String street_no) {
        this.street_no = street_no;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public Long getParentOrganisation() {
        return parentOrganisation;
    }

    public void setParentOrganisation(Long parentOrganisation) {
        this.parentOrganisation = parentOrganisation;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }
    @Override
    public String toString(){
        String retStr = null;
        ObjectMapper m = new ObjectMapper();
        try{

            retStr = m.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        }
        catch(Exception e){
            System.out.println("Could not convert to JSON: " + e.toString());
        }
        return retStr;
    }
}
