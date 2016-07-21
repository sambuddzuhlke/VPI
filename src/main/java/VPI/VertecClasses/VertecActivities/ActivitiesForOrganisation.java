package VPI.VertecClasses.VertecActivities;

import VPI.Entities.Activity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ActivitiesForOrganisation {

    private Long organisationId;
    private String name;
    private List<VPI.VertecClasses.VertecActivities.Activity> activitiesForOrganisation;

    public ActivitiesForOrganisation() {
    }

    public ActivitiesForOrganisation(Long organisationId, String name) {
        this.organisationId = organisationId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(Long organisationId) {
        this.organisationId = organisationId;
    }

    public List<VPI.VertecClasses.VertecActivities.Activity> getActivitiesForOrganisation() {
        return activitiesForOrganisation;
    }

    public void setActivitiesForOrganisation(List<VPI.VertecClasses.VertecActivities.Activity> activitiesForOrganisation) {
        this.activitiesForOrganisation = activitiesForOrganisation;
    }

    public String toJSONString() {
        String retStr = null;
        ObjectMapper m = new ObjectMapper();
        try {

            retStr = m.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            System.out.println("Could not convert XML Envelope to JSON: " + e.toString());
        }
        return retStr;
    }
}
