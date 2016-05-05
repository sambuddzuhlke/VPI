package VPI;

import VPI.PDClasses.*;
import VPI.VertecClasses.JSONContact;
import VPI.VertecClasses.JSONOrganisation;
import VPI.VertecClasses.VertecService;
import VPI.VertecClasses.ZUKResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by sabu on 29/04/2016.
 */
public class VertecSynchroniser {

    private PDService PDS;
    private VertecService VS;

    public List<PDContactSend> contactPutList;
    public List<PDContactSend> contactPostList;

    public List<PDOrganisationSend> organisationPutList;
    public List<JSONOrganisation> organisationPostList;

    private Map<Long,Long> teamIdMap;

    public VertecSynchroniser() {
        RestTemplate restTemplate = new RestTemplate();
        MyCredentials creds = new MyCredentials();
        this.PDS = new PDService(restTemplate, "https://api.pipedrive.com/v1/", creds.getApiKey());
        this.VS = new VertecService("localhost:9999");
        this.contactPostList = new ArrayList<>();
        this.contactPutList = new ArrayList<>();
        this.organisationPostList = new ArrayList<>();
        this.organisationPutList = new ArrayList<>();
        this.teamIdMap = setupTeamMap();
    }

    public List<List<Long>> importToPipedrive() {
        //get all Vertec Data
        ZUKResponse allVertecData = VS.getZUKinfo().getBody();
        //get all Pipedrive organisations
        List<PDOrganisation> pipedriveOrgs = PDS.getAllOrganisations().getBody().getData();
        //compare pipedrive orgs along with nested contacts, removing nested contacts from contacts
        resolveOrganisationsAndNestedContacts(allVertecData.getOrganisationList(), pipedriveOrgs);

        //get all pipedrive contacts, filter to only use those without organisations
        List<PDContactReceived> pipedriveContacts = PDS.getAllContacts().getBody().getData();
        List<PDContactReceived> contactsWithoutOrg = filterContactsWithOrg(pipedriveContacts);
        //compare dangling vcontacts to leftover pdcontacts
        compareContacts(allVertecData.getDanglingContacts(), contactsWithoutOrg);

        List<List<Long>> ids = new ArrayList<>();

        //now ready to post/put
        List<List<Long>> orgsNConts = postVOrganisations();
        List<Long> orgsPut = putPdOrganisations();
        List<Long> contsPost = postContacts();
        List<Long> contsPut = putContacts();
        ids.add(orgsNConts.get(0));
        ids.add(orgsPut);
        ids.add(orgsNConts.get(1));
        ids.add(contsPost);
        ids.add(contsPut);

        return ids;
    }

    public void resolveOrganisationsAndNestedContacts(List<JSONOrganisation> vOrgs, List<PDOrganisation> pOrgs) {
        for(JSONOrganisation vo : vOrgs){
            Boolean matched = false;
            for(PDOrganisation po : pOrgs){
                if(po.getV_id() == null) continue; //TODO: Modify to add them to inconsitstency report
                if(vo.getObjid().longValue() == po.getV_id().longValue()){
                    matched = true;
                    compareOrganisationDetails(vo, po);
                    resolveContactsForOrgs(vo,po);
                }
            }
            if(!matched){
                organisationPostList.add(vo);
            }

        }
    }
    public void testresolveOrganisationsAndNestedContacts(List<JSONOrganisation> vOrgs, List<PDOrganisation> pOrgs) {
        for(JSONOrganisation vo : vOrgs){
            Boolean matched = false;
            for(PDOrganisation po : pOrgs){
                if(po.getV_id() == null) continue; //TODO: Modify to add them to inconsitstency report
                if(vo.getObjid().longValue() == po.getV_id().longValue()){
                    matched = true;
                    compareOrganisationDetails(vo, po);
                    resolveTestContactsForOrgs(vo,po);
                }
            }
            if(!matched){
                organisationPostList.add(vo);
            }

        }
    }

    private Boolean compareOrganisationDetails(JSONOrganisation vo, PDOrganisation po){
        Boolean diff = false;
        if(! vo.getFormattedAddress().equals(po.getAddress())) diff = true;
        if( ! vo.getName().equals(po.getName())) diff = true;
        if( teamIdMap.get(vo.getOwner()) != po.getOwner_id().getId()) diff = true;


        if(diff){
            Long ownerid = teamIdMap.get(vo.getOwner());
            organisationPutList.add(new PDOrganisationSend(vo,po,ownerid)); //TODO: Make constructor deal with most recent
        }

        return diff;
    }

    public void resolveTestContactsForOrgs(JSONOrganisation jo, PDOrganisation po){
    }

    private void resolveContactsForOrgs(JSONOrganisation jo, PDOrganisation po){
        List<PDContactReceived>  pdContacts = PDS.getContactsForOrganisation(po.getId()).getBody().getData();
        compareContacts(jo.getContacts(), pdContacts);
    }
    public void compareContacts(List<JSONContact> vConts, List<PDContactReceived> pContacts) {

        for(JSONContact vc : vConts) {
            Boolean matchedName = false;
            Boolean modified = false;
            PDContactReceived temp = null;
            Long tempOrgID = null;
            if(pContacts == null) continue;
            for(PDContactReceived pc : pContacts) {

                String fullname = vc.getFirstName() + " " + vc.getSurname();

                if (pc.getOrg_id() != null && pc.getOrg_id().getValue() != null) tempOrgID = pc.getOrg_id().getValue();

                if(pc.getV_id() == null) continue; //TODO: Modify to support inconsistency Report
                if (vc.getObjid().longValue() == pc.getV_id().longValue()) {
                    matchedName = true;

                    //resolve internal contact details;
                    modified = resolveContactDetails(vc, pc);
                    if(modified) {
                        temp = pc;
                    }
                }

            }
            if (!matchedName) {
                Long owner = teamIdMap.get(vc.getOwner());
                PDContactSend newContact = new PDContactSend(vc,owner);
                newContact.setOrg_id(tempOrgID);
                newContact.setOwner_id(teamIdMap.get(vc.getOwner()));
                contactPostList.add(newContact);
            }
            if(modified) {
                contactPutList.add(new PDContactSend(temp));
            }

        }



    }

    public Boolean resolveContactDetails(JSONContact v, PDContactReceived p){

        Boolean modifiedPhone = false;

        if(v.getMobile() != null) {
            Boolean matchedMobile = false;
            for(ContactDetail pph: p.getPhone()) {
                if(v.getMobile().equals(pph.getValue())) {
                    matchedMobile = true;
                }
            }
            if(!matchedMobile) {
                p.getPhone().add(new ContactDetail(v.getMobile(), false));
                modifiedPhone = true;
            }
        }


        if(v.getPhone() != null) {
            Boolean matchedPhone = false;
            for(ContactDetail pph: p.getPhone()) {
                if(v.getPhone().equals(pph.getValue())) {
                    matchedPhone = true;
                    if (!pph.getPrimary()) {
                        pph.setPrimary(true);
                        modifiedPhone = true;
                    }
                } else {
                    pph.setPrimary(false);
                }
            }
            if (!matchedPhone) {
                p.getPhone().add(new ContactDetail(v.getPhone(), true));
                modifiedPhone = true;
            }
        }

        Boolean modifiedEmail = false;

        if (v.getEmail() != null) {

            Boolean matchedEmail = false;
            for (ContactDetail pe : p.getEmail()) {
                if (v.getEmail().equals(pe.getValue())) {
                    matchedEmail = true;
                    if (!pe.getPrimary()) {
                        pe.setPrimary(true);
                        modifiedEmail = true;
                    }
                } else {
                    pe.setPrimary(false);
                }
            }
            if (!matchedEmail) {
                p.getEmail().add(new ContactDetail(v.getEmail(), true));
                modifiedEmail = true;
            }
        }

        Boolean matchedName = false;
        String fullName = v.getFirstName() + " " + v.getSurname();

        if( ! fullName.equals(p.getName())) matchedName = true;


        return modifiedEmail || modifiedPhone || matchedName;
    }

    //removes Contacts that are attached to organisations (as they are already handled
    public List<PDContactReceived> filterContactsWithOrg(List<PDContactReceived> pContacts) {
        //do more somthings
        List<PDContactReceived> filteredList = new ArrayList<>();

        for(PDContactReceived c : pContacts) {
            if(c.getOrg_id() == null) {
                filteredList.add(c);
            }
        }
        return filteredList;
    }

    public List<List<Long>> postVOrganisations(){

        ResponseEntity<PDOrganisationResponse> res = null;
        List<List<Long>> both = new ArrayList<>();
        List<Long> orgsPosted = new ArrayList<>();
        List<Long> contactsPosted = new ArrayList<>();
        for(JSONOrganisation o : organisationPostList){
            Long ownerid = teamIdMap.get(o.getOwner());
            res = PDS.postOrganisation(new PDOrganisationSend(o, ownerid));
            orgsPosted.add(res.getBody().getData().getId());

            for(JSONContact c : o.getContacts()){
                Long owner = teamIdMap.get(c.getOwner());
                PDContactSend s = new PDContactSend(c,owner);
                s.setOrg_id(res.getBody().getData().getId());
                contactsPosted.add(PDS.postContact(s).getBody().getData().getId());
            }
        }
        both.add(orgsPosted);
        both.add(contactsPosted);
        return both;
    }

    public List<Long> putPdOrganisations(){

        return PDS.putOrganisationList(organisationPutList);
    }

    public List<Long> postContacts(){

        return PDS.postContactList(contactPostList);
    }

    public List<Long> putContacts(){

        return PDS.putContactList(contactPutList);
    }

    public Map<Long,Long> setupTeamMap(){
        Map<Long,Long> map = new HashMap<>();
        map.put(5295L, 1363410L); //Wolfgang
        map.put(504149L, 1363402L); //Tim
        map.put(12456812L, 136429L); //Neil
        map.put(6574798L, 1363424L); //Mike
        map.put(8619482L, 1363416L); //Justin
        map.put(16887415L, 1363403L); //Brewster
        map.put(504354L, 1363488L); //Keith
        map.put(16400137L, 1277584L); //Peter Brown
        map.put(17739496L, 1277584L); //Steve Freeman
        map.put(22501574L, 1277584L); //John Seston
        map.put(2350788L, 1277584L); //Sabine
        map.put(24807265L, 1277584L); //Ileana
        map.put(24907657L, 1277584L); //Ina
        map.put(null, 1277584L); //null

        return map;
    }

    public void clear(){
        this.organisationPostList.clear();
        this.organisationPutList.clear();
        this.contactPostList.clear();
        this.contactPutList.clear();
    }


    public PDService getPDS() {
        return PDS;
    }

    public void setPDS(PDService PDS) {
        this.PDS = PDS;
    }

    public VertecService getVS() {
        return VS;
    }

    public void setVS(VertecService VS) {
        this.VS = VS;
    }
}
