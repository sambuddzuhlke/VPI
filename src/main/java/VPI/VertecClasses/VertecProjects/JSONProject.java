package VPI.VertecClasses.VertecProjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by sabu on 16/05/2016.
 */
public class JSONProject {

    @JsonProperty("title")
    private String title;
    @JsonProperty("v_id")
    private Long v_id;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("code")
    private String code;
    @JsonProperty("client_ref")
    private Long clientRef;
    @JsonProperty("customer_ref")
    private Long customerRef;
    @JsonProperty("leader_ref")
    private Long leaderRef;
    @JsonProperty("phases")
    private List<JSONPhase> phases;

    public JSONProject() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getV_id() {
        return v_id;
    }

    public void setV_id(Long v_id) {
        this.v_id = v_id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getClientRef() {
        return clientRef;
    }

    public void setClientRef(Long clientRef) {
        this.clientRef = clientRef;
    }

    public Long getLeaderRef() {
        return leaderRef;
    }

    public void setLeaderRef(Long leaderRef) {
        this.leaderRef = leaderRef;
    }

    public List<JSONPhase> getPhases() {
        return phases;
    }

    public void setPhases(List<JSONPhase> phases) {
        this.phases = phases;
    }

    public Long getCustomerRef() {
        return customerRef;
    }

    public void setCustomerRef(Long customerRef) {
        this.customerRef = customerRef;
    }
}
