package VPI;

import VPI.PDClasses.PDOrganisation;
import VPI.VClasses.VOrganisation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sabu on 15/04/2016.
 */
public class Organisations {

    public List<VOrganisation> vOrganisations;
    public List<PDOrganisation> pdOrganisations;

    public List<PDOrganisation> postList;
    public List<PDOrganisation> putList;

    public Organisations() {
        this.pdOrganisations = new ArrayList<>();
        this.vOrganisations  = new ArrayList<>();
        this.postList        = new ArrayList<>();
        this.putList         = new ArrayList<>();
    }
}
