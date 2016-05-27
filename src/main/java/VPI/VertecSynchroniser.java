package VPI;

import VPI.PDClasses.Activities.PDActivityReceived;
import VPI.PDClasses.Activities.PDActivitySend;
import VPI.PDClasses.Contacts.ContactDetail;
import VPI.PDClasses.Contacts.PDContactReceived;
import VPI.PDClasses.Contacts.PDContactSend;
import VPI.PDClasses.Deals.PDDealReceived;
import VPI.PDClasses.Deals.PDDealSend;
import VPI.PDClasses.Organisations.PDOrganisationReceived;
import VPI.PDClasses.Organisations.PDOrganisationResponse;
import VPI.PDClasses.Organisations.PDOrganisationSend;
import VPI.PDClasses.Organisations.PDRelationship;
import VPI.PDClasses.PDService;
import VPI.PDClasses.Users.PDUser;
import VPI.VertecClasses.VertecActivities.JSONActivity;
import VPI.VertecClasses.VertecOrganisations.JSONContact;
import VPI.VertecClasses.VertecOrganisations.JSONOrganisation;
import VPI.VertecClasses.VertecOrganisations.ZUKOrganisations;
import VPI.VertecClasses.VertecProjects.JSONPhase;
import VPI.VertecClasses.VertecProjects.JSONProject;
import VPI.VertecClasses.VertecService;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class VertecSynchroniser {

    private PDService PDS;
    private VertecService VS;

    public List<PDContactSend> contactPutList;
    public List<PDContactSend> contactPostList;

    public List<PDOrganisationSend> organisationPutList;
    public List<JSONOrganisation> organisationPostList;

    public List<PDDealSend> dealPostList;
    public List<PDDealSend> dealPutList;

    private Map<String,Long> teamIdMap;
    //map of v_id's to p_ids used for building relationship heirarchy
    private Map<Long, Long> orgIdMap;
    //map of v_ids to p_ids for contacts, for associating deals with persons
    private Map<Long, Long> contactIdMap;
    private List<PDActivitySend> activityPostList;
    private List<PDActivitySend> activityPutList;
    private HashMap<String, String> typeMap;
    private HashMap<Long, Long> dealIdMap;


    public VertecSynchroniser() {

        //retrieve credentials from locally stored file
        MyCredentials creds = new MyCredentials();

        //set up Pipedrive and Vertec services
        this.PDS = new PDService("https://api.pipedrive.com/v1/", creds.getApiKey());
        this.VS = new VertecService("localhost:9999");

        //initialise Contact/Og put and post list (for sending to pipedrive);
        this.contactPostList = new ArrayList<>();
        this.contactPutList = new ArrayList<>();
        this.organisationPostList = new ArrayList<>();
        this.organisationPutList = new ArrayList<>();
        this.dealPostList = new ArrayList<>();
        this.dealPutList = new ArrayList<>();
        this.activityPostList = new ArrayList<>();
        this.activityPutList = new ArrayList<>();

        this.teamIdMap = new HashMap<>();
        this.orgIdMap = new HashMap<>();
        this.contactIdMap = new HashMap<>();
        this.typeMap = new HashMap<>();
    }

    @SuppressWarnings("unused")
    public List<List<Long>> importOrganisationsAndContactsToPipedrive() {

        //get all Vertec Data
        ZUKOrganisations allVertecData = VS.getZUKOrganisations().getBody();

        //constructTeamIdMap(getVertecUserEmails(allVertecData), getPipedriveUsers()); //TODO: use this instead of constructTestTeamMap() on deployment
        constructTestTeamMap();

        //get all Pipedrive organisations

        //compare pipedrive orgs along with nested contacts, removing nested contacts from contacts
        resolveOrganisationsAndNestedContacts(
                allVertecData.getOrganisationList(),
                PDS.getAllOrganisations().getBody().getData());

        //get all pipedrive contacts, filter to only use those without organisations

        //compare dangling vcontacts to leftover pdcontacts
        compareContacts(
                allVertecData.getDanglingContacts(),
                filterContactsWithOrg(PDS.getAllContacts().getBody().getData()));


        //initialize return list
        List<List<Long>> ids = new ArrayList<>();

        //now ready to post/put
        List<List<Long>> orgsNConts = postVOrganisations();

        List<Long> orgsPut = putPdOrganisations();
        this.contactIdMap.putAll(postContacts());
        Map<Long, Long> putMap = putContacts();
        this.contactIdMap.putAll(putMap);

        //get list of pd relationships, then post them
        List<PDRelationship> relationships = getOrganistionHeirarchy(allVertecData.getOrganisationList());
        postRelationshipList(relationships);

        //return list of orgs and contact ids that have been posted/edited to pipedrive
        ids.add(orgsNConts.get(0));
        ids.add(orgsPut);
        ids.add(orgsNConts.get(1));
        ids.add(contactIdMap.values().stream().collect(toList()));
        ids.add(putMap.values().stream().collect(toList()));

        return ids;
    }
//    public List<List<Long>> importOrganisationsAndContactsToPipedriveAndPrint() {
//
//        long startTime = System.nanoTime();
//        //get all Vertec Data
//        System.out.println("Getting ZUK data from vertec");
//        ZUKOrganisations allVertecData = VS.getZUKOrganisations().getBody();
//        long zukEndTime = System.nanoTime();
//        System.out.println("Took " + ((zukEndTime - startTime)/1000000) + " milliseconds");
//
//        Set<String> v_emails = getVertecUserEmails(allVertecData);
//        List<PDUser> pd_users= getPipedriveUsers();
//
//        //constructTeamIdMap(v_emails, pd_users); //TODO: use this instead of constructTestTeamMap() on deployment
//        System.out.println("Contructing hardcoded team map");
//        constructTestTeamMap();
//        long teamEnd = System.nanoTime();
//        System.out.println("Took " + ((teamEnd - zukEndTime)/1000000) + " milliseconds");
//
//        //get all Pipedrive organisations
//        System.out.println("Getting all organisations from pipedrive");
//        List<PDOrganisationReceived> pipedriveOrgs = PDS.getAllOrganisations().getBody().getData();
//        long pdorgTime = System.nanoTime();
//        System.out.println("Took " + ((pdorgTime - teamEnd)/1000000) + " milliseconds");
//
//        //compare pipedrive orgs along with nested contacts, removing nested contacts from contacts
//        System.out.println("About to attempt to resolve organisations and their nested contacts");
//        resolveOrganisationsAndNestedContacts(allVertecData.getOrganisationList(), pipedriveOrgs);
//        long resolveOrgTime = System.nanoTime();
//        System.out.println("Took " + ((resolveOrgTime - pdorgTime)/1000000) + " milliseconds");
//
//        //get all pipedrive contacts, filter to only use those without organisations
//        System.out.println("Getting all contacts from pipedrive");
//        List<PDContactReceived> pipedriveContacts = PDS.getAllContacts().getBody().getData();
//        long pdcontTime = System.nanoTime();
//        System.out.println("Took " + ((pdcontTime - resolveOrgTime)/1000000) + " milliseconds");
//        System.out.println("Getting all contacts (not attached to organisations) from vertec from ZUK info");
//        List<PDContactReceived> contactsWithoutOrg = filterContactsWithOrg(pipedriveContacts);
//        long vcontTime = System.nanoTime();
//        System.out.println("Took " + ((vcontTime - pdcontTime)/1000000) + " milliseconds");
//
//        //compare dangling vcontacts to leftover pdcontacts
//        System.out.println("About to compare contacts");
//        compareContacts(allVertecData.getDanglingContacts(), contactsWithoutOrg);
//        long compCTime = System.nanoTime();
//        System.out.println("Took " + ((compCTime - vcontTime)/1000000) + " milliseconds");
//
//
//        //initialize return list
//        List<List<Long>> ids = new ArrayList<>();
//
//        //now ready to post/put
//        System.out.println("Posting Organisations");
//        List<List<Long>> orgsNConts = postVOrganisations();
//        long orgPostTime = System.nanoTime();
//        System.out.println("Took " + ((orgPostTime - compCTime)/1000000) + " milliseconds");
//        System.out.println("Posted: " + orgsNConts.get(0).size() + " orgs, and " + orgsNConts.get(1).size() + " contacts");
//        System.out.println("Putting Organisations");
//        List<Long> orgsPut = putPdOrganisations();
//        long orgPutTime = System.nanoTime();
//        System.out.println("Took " + ((orgPutTime - orgPostTime)/1000000) + " milliseconds");
//        System.out.println("Putted:" + orgsPut.size());
//        System.out.println("Posting Dangling Contacts");
//        this.contactIdMap = postContacts();
//        long contPostTime = System.nanoTime();
//        System.out.println("Took " + ((contPostTime - orgPutTime)/1000000) + " milliseconds");
//        System.out.println("Posted: " + contactIdMap.values().size());
//        System.out.println("Putting Dangling Contacts");
//        Map<Long, Long> putMap = putContacts();
//        this.contactIdMap.putAll(putMap);
//        long contputTime = System.nanoTime();
//        System.out.println("Took " + ((contputTime - contPostTime)/1000000) + " milliseconds");
//        System.out.println("Putted: " + putMap.values().size());
//
//        //get list of pd relationships, then post them
//        System.out.println("Building relationship hierarchy");
//        List<PDRelationship> relationships = getOrganistionHeirarchy(allVertecData.getOrganisationList());
//        long relTime = System.nanoTime();
//        System.out.println("Took " + ((relTime - contputTime)/1000000) + " milliseconds");
//        System.out.println("Found " + relationships.size() + " relationships, now posting them");
//        postRelationshipList(relationships);
//        long relPostTime = System.nanoTime();
//        System.out.println("Took " + ((relPostTime - relTime)/1000000) + " milliseconds");
//
//        //return list of orgs and contact ids that have been posted/edited to pipedrive
//        ids.add(orgsNConts.get(0));
//        ids.add(orgsPut);
//        ids.add(orgsNConts.get(1));
//        //ids.add((Set<Long>) contactIdMap.values());
//        //ids.add((List) putMap.values());
//
//        System.out.println("Done!");
//
//        long endTime = System.nanoTime();
//        System.out.println("Took " + ((endTime - startTime)/6000000000L) + " milliseconds");
//
//        return ids;
//    }

    public List<Long> importProjectsAndPhasesToPipedrive() {
        compareDeals(
                createDealObjects(VS.getZUKProjects().getBody().getProjects()),
                PDS.getAllDeals().getBody().getData());

        System.out.println("That was interesting, found " + dealPostList.size() + " new deals, and " + dealPutList.size() + " deals to update");

        dealIdMap = PDS.postDealList(dealPostList);
        List<Long> projPut = PDS.updateDealList(dealPutList);
        projPut.addAll(dealIdMap.values());

        return projPut;
    }

    public List<Long> importActivitiesToPipedrive() {

        constructActivityTypeMap();

        compareActivities(VS.getZUKActivities().getBody().getActivityList(), PDS.getAllActivities());

        List<Long> activitiesPosted = PDS.postActivityList(activityPostList);
        System.out.println("Posting " + activitiesPosted.size() + " Activities");
        activitiesPosted.addAll(PDS.putActivitesList(activityPutList));
        return activitiesPosted;
    }

    private void constructActivityTypeMap() {

        String vType = "{362309 : Vertrag / Contract\n" + //Filtered out
                "{362308 : Organigramm / Organizational Chart\n" +
                "{573113 : Auftragsbestätigung / Order Confirmation\n" +
                "{362307 : Angebot / Offer\n" +
                "{586078 : Kundenfeedback / Customer Feedback\n" +
                "{505823 : Eventteilnahme / Event Participation\n" +
                "{279647 : Dokument / Document\n" + //Filtered out
                "{270569 : Sales"; //TODO:ask if we should filter

        typeMap.put("Meeting", "meeting");
        typeMap.put("Aufgabe / Task", "task");
        typeMap.put("EMail", "email");


    }

    private void compareActivities(List<JSONActivity> vActivities, List<PDActivityReceived> pActivities) {

        for (JSONActivity va : vActivities.stream().filter(a -> a.getTitle() != null).collect(toList())) {
            Boolean matched = false;
            Boolean modified = false;
            PDActivityReceived temp = null;
            for(PDActivityReceived pa : pActivities) {
                if (va.getId().longValue() == extractVID(pa.getNote()) && va.getDone() == pa.getDone()) {
                    matched = true;
//                    modified = compareActivityDetails(va, pa);
//                    if (modified) temp = pa;
                }
            }
            if (!matched) {
                Long user_id = teamIdMap.get(va.getAssignee());

                //attempt to get customer link from contact map, if fails try from org map (we dont know which type it will be)
                Long contact_id = contactIdMap.get(va.getCustomer_link());
                Long org_id = orgIdMap.get(va.getCustomer_link());
                //attempt to get link to project phase, if fails then try link to project which will link to all phases;
                Long deal_id = dealIdMap.get(va.getProject_phase_link());
                //TODO: find out how to handle missing phase links
                //attempt to get type, if fails add to misc type instead
                String type = typeMap.get(va.getType());
                if (type == null) type = "misc";
                this.activityPostList.add(new PDActivitySend(va, user_id, contact_id, org_id, deal_id, type));
            }
            if (modified) {
                //TODO: handle changing activities?
            }

        }

    }

    private Boolean compareActivityDetails(JSONActivity va, PDActivityReceived pa) {
        return !(va.getTitle().equals(pa.getSubject())
                        && va.getText().equals(getNoteFromNoteWithVID(pa.getNote()))
                        && teamIdMap.get(va.getAssignee()).equals(pa.getUser_id()) &&
                        (contactIdMap.get(va.getCustomer_link()).equals(pa.getPerson_id()) //TODO: fix
                                || orgIdMap.get(va.getCustomer_link()).equals(pa.getOrg_id()))
                        && va.getDone() == pa.getDone()
                        && (pa.getType()).equals(typeMap.get(va.getType()))
                        && dealIdMap.get(va.getProject_phase_link()).longValue() == pa.getDeal_id()
                //TODO: handle project if we decide to link to project if phase link null
                );
    }

    private String getNoteFromNoteWithVID(String note) {
        if (extractVID(note) == -1) {
            return note;
        } else {
            String[] vIdAndRest = note.split("#");
            return vIdAndRest[1];
        }
    }

    public Long extractVID(String note) {
        if (note.contains("V_ID:")) {
            String[] vIdAndRest = note.split("#");
            String vIdString = vIdAndRest[0];
            String[] keyValue = vIdString.split(":");
            if (keyValue.length == 2) {
                return Long.parseLong(keyValue[1]);
            }
        }
        return -1L;
    }

    public void compareDeals(List<PDDealSend> vertecDeals, List<PDDealReceived> pipedriveDeals) {
        for (PDDealSend vDeal : vertecDeals) {
            Boolean matched = false;
            Boolean modified = false;
            PDDealReceived temp = null;
            for (PDDealReceived pDeal : pipedriveDeals) {
                //if phase code matches then check details
                if (vDeal.getProject_number().equals(pDeal.getProject_number())
                        && vDeal.getPhase().equals(pDeal.getPhase())) {
                    matched = true;

                    modified = compareDealDetails(vDeal, pDeal);
                    if (modified) {
                        temp = pDeal;
                    }
                }
            }

            if (!matched) {
                this.dealPostList.add(vDeal);
            }
            if (modified) {
//                try {
//                    DateTimeFormatter p = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss");
//                    DateTimeFormatter v = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//
//                    LocalDateTime pt = LocalDateTime.from(p.parse(temp.getUpdate_time()));
//                    LocalDateTime vt = LocalDateTime.from(v.parse(vDeal.getModified()));
//
//                    if (pt.isAfter(vt)) {
//
//                        Boolean filled = addMissingInformation(vDeal, temp);
//                        if (filled) {
//                            this.dealPutList.add(new PDDealSend(temp));
//                        }
//
//                    } else {

                        vDeal.setId(temp.getId());
                        vDeal.setLead_type(temp.getLead_type());
                        vDeal.setZuhlke_office(temp.getZuhlke_office());
                        this.dealPutList.add(vDeal);
//
//                    }
//                } catch (Exception e) {
//                    System.out.println("Caught Exception comparing deals: " + e);
//                }

            }

        }

    }

    public Boolean compareDealDetails(PDDealSend vDeal, PDDealReceived pDeal) {
        Boolean diff = false;
        if ( ! vDeal.getTitle().equals(pDeal.getTitle())) diff = true;
        if ( ! vDeal.getValue().equals(pDeal.getValue())) diff = true;
        if ( ! vDeal.getCurrency().equals(pDeal.getCurrency())) diff = true;
        if (vDeal.getUser_id().longValue() != pDeal.getUser_id().getId()) diff = true;
        if(pDeal.getPerson_id() != null
                && pDeal.getPerson_id().getValue() != null
                && vDeal.getPerson_id() != null
                && vDeal.getPerson_id() != pDeal.getPerson_id().getValue().longValue()) diff = true;
        if(pDeal.getOrg_id() != null
                && pDeal.getOrg_id().getValue() != null
                && vDeal.getOrg_id() != null
                && vDeal.getOrg_id() != pDeal.getOrg_id().getValue().longValue()) diff = true;
        if(vDeal.getStage_id() != pDeal.getStage_id()) diff = true;
        if(! vDeal.getStatus().equals(pDeal.getStatus())) diff = true;

        return diff;
    }

    public List<PDDealSend> createDealObjects(List<JSONProject> projects) {

        List<PDDealSend> deals = new ArrayList<>();

        for(JSONProject project : projects) {

            for(JSONPhase phase : project.getPhases()) {

                PDDealSend deal = new PDDealSend();
                /**
                 * must set:
                 * title --------- done
                 * value --------- done
                 * currency ------ done
                 * user_id ------- done (assuming teamMap is populated)
                 * person_id -----
                 * org_id -------- done (assuming orgIdMap is populated (check))
                 * stage_id ------ done
                 * lost_reason?--- done
                 * status -------- done
                 * add_time ------ done
                 * visible_to ---- done
                 * v_id ---------- done
                 * zuhlke_office-- n/a
                 * lead_type ----- n/a
                 * project_number- done
                 * phase --------- done
                 * cost ---------- n/a
                 * cost_currency - n/a
                 * won_time
                 * lost_time
                 */

                //title
                if (project.getTitle() != null && !project.getTitle().equals("")) {
                    String title = project.getTitle() + ": " + phase.getDescription();
                    deal.setTitle(title);
                } else {
                    deal.setTitle(phase.getDescription());
                }

                //value
                String value = phase.getExternalValue();
                deal.setValue(value);

                //currency
                String currency = project.getCurrency();
                deal.setCurrency(currency);

                //sets the person responsible to be the leader of the phase,
                //but if this is null the leader of whole project? or check other phase owners?;
                // TODO: check other phases for user_id if null
                Long user_id = teamIdMap.get(phase.getPersonResponsible());
//TODO: set proper id
                deal.setUser_id(user_id == null ? teamIdMap.get(project.getLeaderRef()) : user_id);

                //person_id
                deal.setPerson_id(contactIdMap.get(project.getCustomerRef()));

                //org_id
                deal.setOrg_id(orgIdMap.get(project.getClientRef()));

                //stage_id
                //TODO: change to correct stage_ids once in production
                String salesStatus = phase.getSalesStatus();
                String code = salesStatus.substring(0, Math.min(salesStatus.length(), 2));
                Integer num = Integer.parseInt(code);

                //for setting status
                String status = "open";

                switch (num) {
                    //Exploratory = 1, NewLead/Extension = 2, QualifiedLead = 3
                    case 5: deal.setStage_id(2);
                        break;
                    //Rfp Recieved = 3
                    case 10: deal.setStage_id(4);
                        break;
                    //Offered = 6
                    case 11: deal.setStage_id(5);
                        break;
                    //UnderNegotiation = 5
                    case 12: deal.setStage_id(7);
                        break;
                    //VerballySold = 7
                    case 20: deal.setStage_id(6);
                        break;
                    //SOLD = WON
                    case 21: status = "won";
                        break;
                    //LOST = LOST
                    case 30: status = "lost";
                        deal.setLost_reason(phase.getLostReason()); //TODO: add lost reason map
                        deal.setLost_time(phase.getLost_time());
                        break;
                    //FINISHED = WON
                    case 40: status = "won";
                        deal.setWon_time(phase.getWon_time());
                        break;
                    default: System.out.println(num);
                        break;
                }

                //status ('open' = Open, 'won' = Won, 'lost' = Lost, 'deleted' = Deleted)
                //deal.setStatus(status);
                deal.setStatus(status);

                //add_time
                try {
                    String[] dateTime = phase.getCreationDate().split("T");
                    String date = dateTime[0];
                    String time = dateTime[1];
                    String newDateTime = date + " " + time;
                    deal.setAdd_time(newDateTime);
                } catch (Exception e) {
                    deal.setAdd_time("2000-01-01 00:00:00");
                }

                //visible_to (1 = owner and followers, 3 = everyone)
                deal.setVisible_to(3);

                //v_id
                deal.setV_id(phase.getV_id());

                //project number
                deal.setProject_number(project.getCode());

                //phase
                String dealPhase = phase.getCode();
                deal.setPhase(dealPhase);

                //cost
                //TODO: Ensure we can ignore cost, lead type and zuhlke office

                //modified
                try {
                    String[] dateTime = phase.getModifiedDate().split("T");
                    String date = dateTime[0];
                    String time = dateTime[1];
                    String newDateTime = date + " " + time;
                    deal.setModified(newDateTime);
                } catch (Exception e) {
                    deal.setModified("2000-01-01 00:00:00");
                }

                if (deal.getUser_id() == null) {
                    System.out.println(deal);
                }
                //add deal to list
                deals.add(deal);
            }


        }

        return deals;
    }

    private Boolean addMissingInformation(PDDealSend vDeal, PDDealReceived pdDeal){
        Boolean filled = false;
        if(pdDeal.getValue() == null) {
            pdDeal.setValue(vDeal.getValue());
            filled = true;
        }
        if(pdDeal.getCurrency() == null) {
            pdDeal.setCurrency(vDeal.getCurrency()); filled = true;
        }
        if(pdDeal.getUser_id() == null) {
            pdDeal.getUser_id().setId(vDeal.getUser_id()); filled = true;
        }
        if(pdDeal.getUser_id() == null) {
            pdDeal.getUser_id().setId(vDeal.getUser_id()); filled = true;
        }
        if(pdDeal.getPerson_id() == null) {
            pdDeal.getPerson_id().setValue(vDeal.getPerson_id()); filled = true;
        }
        if(pdDeal.getOrg_id() == null) {
            pdDeal.getOrg_id().setValue(vDeal.getOrg_id()); filled = true;
        }
        if(pdDeal.getLost_reason() == null) {
            pdDeal.setLost_reason(vDeal.getLost_reason()); filled = true;
        }
        if(pdDeal.getVisible_to() == null) {
            pdDeal.setVisible_to(vDeal.getVisible_to().toString()); filled = true;
        }
        if(pdDeal.getVisible_to() == null){
            pdDeal.setVisible_to(vDeal.getVisible_to().toString()); filled = true;
        }
        if(pdDeal.getZuhlke_office() == null){
            pdDeal.setZuhlke_office(vDeal.getZuhlke_office()); filled = true;
        }

        return filled;
    }


    public List<PDRelationship> getOrganistionHeirarchy(List<JSONOrganisation> orgs) {

        List<PDRelationship> rels = new ArrayList<>();

        for (JSONOrganisation org : orgs) {

            Long childOrgPId = orgIdMap.get(org.getObjid());
            Long parentOrgPId = orgIdMap.get(org.getParentOrganisationId());

            if (childOrgPId != null && parentOrgPId != null) {

                PDRelationship rel = new PDRelationship(parentOrgPId, childOrgPId);
                rels.add(rel);

            }

        } //TODO: get all organisations reltionships and compare as pipedrive thorws bad request if rel already exists

        return rels;

    }

    //TODO: at some point work out exactly what eachof the post functions should return;
    public void postRelationshipList(List<PDRelationship> relationships) {

        for (PDRelationship rel : relationships) {

            PDS.postOrganisationRelationship(rel);

        }

    }

    public void resolveOrganisationsAndNestedContacts(List<JSONOrganisation> vOrgs, List<PDOrganisationReceived> pOrgs) {
        for(JSONOrganisation vo : vOrgs){
            Boolean matched = false;
            for(PDOrganisationReceived po : pOrgs){
                if(po.getV_id() == null) continue;
                if(vo.getObjid().longValue() == po.getV_id().longValue()){
                    matched = true;
                    orgIdMap.put(vo.getObjid(), po.getId());
                    compareOrganisationDetails(vo, po);

                    //TODO: Change so that all contacts get compared as to handle contacts changing organisations
                    resolveContactsForOrgs(vo,po);
                }
            }
            if(!matched){
                organisationPostList.add(vo);
            }

        }
    }

    public void testresolveOrganisationsAndNestedContacts(List<JSONOrganisation> vOrgs, List<PDOrganisationReceived> pOrgs) {
        for(JSONOrganisation vo : vOrgs){
            Boolean matched = false;
            for(PDOrganisationReceived po : pOrgs){
                if(po.getV_id() == null) continue;
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

    private Boolean compareOrganisationDetails(JSONOrganisation vo, PDOrganisationReceived po){
        Boolean diff = false;
        if(! vo.getFormattedAddress().equals(po.getAddress())) diff = true;
        if( ! vo.getName().equals(po.getName())) diff = true;
        Long user_id = teamIdMap.get(vo.getOwner());
        if (user_id == null) {
            diff = true;
        } else {
            if( vo
                    .getOwner() != null
                    && po.getOwner_id().getId().longValue()
                    != user_id) diff = true;
        }

        if(diff){
            Long ownerid = user_id == null ? po.getOwner_id().getId() : teamIdMap.get(vo.getOwner());
            organisationPutList.add(new PDOrganisationSend(vo,po,ownerid)); //TODO: Make constructor deal with most recent
        }

        return diff;
    }

    public void resolveTestContactsForOrgs(JSONOrganisation jo, PDOrganisationReceived po){
    }

    private void resolveContactsForOrgs(JSONOrganisation jo, PDOrganisationReceived po){
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

                if (pc.getOrg_id() != null && pc.getOrg_id().getValue() != null) tempOrgID = pc.getOrg_id().getValue();

                if(pc.getV_id() == null) continue;
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
                Long owner = teamIdMap.get(vc.getOwner().toLowerCase());
                PDContactSend newContact = new PDContactSend(vc,owner);
                newContact.setOrg_id(tempOrgID);
                newContact.setOwner_id(teamIdMap.get(vc.getOwner()));
                newContact.setFollowers(new ArrayList<>());
                for(String f : vc.getFollowers()){
                    newContact.getFollowers().add(teamIdMap.get(f));
                }
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

        if( ! fullName.equals(p.getName())) {
            matchedName = true;
        }

        Boolean modifiedOwner = false;

        if(p.getOwner_id()
                .getId()
                != teamIdMap.
                get(v.getOwner()).
                longValue()){
            modifiedOwner = true;
        }


        return modifiedEmail || modifiedPhone || matchedName || modifiedOwner;
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

        int orgCounter = 0;
        int contCounter = 0;

        ResponseEntity<PDOrganisationResponse> res = null;
        List<List<Long>> both = new ArrayList<>();
        List<Long> orgsPosted = new ArrayList<>();
        List<Long> contactsPosted = new ArrayList<>();
        for(JSONOrganisation o : organisationPostList){
            Long ownerid = teamIdMap.get(o.getOwner());
            if (ownerid == null || ownerid == 1277584L) {
                System.out.println(o.toPrettyJSON()); //TODO: remove once figured out
            }
            res = PDS.postOrganisation(new PDOrganisationSend(o, ownerid));
            orgCounter++; //TODO: remove
            orgsPosted.add(res.getBody().getData().getId());

            orgIdMap.put(o.getObjid(), res.getBody().getData().getId());

            for(JSONContact c : o.getContacts()){
                Long owner = teamIdMap.get(c.getOwner());
                PDContactSend s = new PDContactSend(c,owner);
                s.setFollowers(new ArrayList<>());
                for(String f : c.getFollowers()){
                    s.getFollowers().add(teamIdMap.get(f));
                }
                s.setOrg_id(res.getBody().getData().getId());
                Long pdId = PDS.postContact(s).getBody().getData().getId();
                contCounter++;
                this.contactIdMap.put(c.getObjid(), pdId);
                contactsPosted.add(pdId);
            }
        }

        System.out.println("Posted " + orgCounter + " organisations");
        System.out.println("Posted " + contCounter + " contacts attached to organisations");
        both.add(orgsPosted);
        both.add(contactsPosted);
        return both;
    }

    public List<Long> putPdOrganisations(){
        System.out.println("Putting " + organisationPutList.size() + " Organisations");
        return PDS.putOrganisationList(organisationPutList);
    }

    public Map<Long, Long> postContacts(){
        System.out.println("Posting " + contactPostList.size() + " Contacts not attached to organisations");
        return PDS.postContactList(contactPostList);
    }

    public Map<Long, Long> putContacts(){
        System.out.println("Putting " + contactPutList.size() + " Contacts");
        return PDS.putContactList(contactPutList);
    }

    public Map<String, Long> getTeamIdMap() {
        return teamIdMap;
    }

    public void setTeamIdMap(Map<String, Long> teamIdMap) {
        this.teamIdMap = teamIdMap;
    }

    private Set<String> getVertecUserEmails(ZUKOrganisations data) {
        Set<String> v_emails = new HashSet<>();
        for (JSONOrganisation org : data.getOrganisationList()) {

            v_emails.add(org.getOwner());

            for (JSONContact cont : org.getContacts()) {

                v_emails.add(cont.getOwner());

            }

        }

        return v_emails;
    }

    private List<PDUser> getPipedriveUsers() {
        return PDS.getAllUsers().getBody().getData();
    }

    public void constructTeamIdMap(Set<String> v_emails, List<PDUser> pd_users) {//TODO: write test for this
        for (String v_email : v_emails) {
            Boolean mapped = false;
            for (PDUser pd_user : pd_users) {
                if (v_email.toLowerCase().equals(pd_user.getEmail().toLowerCase())) {
                    this.teamIdMap.put(v_email, pd_user.getId());
                    mapped = true;
                }
            }
            if (!mapped) {
                this.teamIdMap.put(v_email, 1363410L ); //TODO: replace id with appropriate id, wolfgangs or admin?
            }
        }
    }

    public void constructTestTeamMap(){
        Map<String,Long> map = new HashMap<>();

        map.put("wolfgang.emmerich@zuhlke.com", 1363410L); //Wolfgang
        map.put("tim.cianchi@zuhlke.com", 1363402L); //Tim
        map.put("neil.moorcroft@zuhlke.com", 1363429L); //Neil
        map.put("mike.hogg@zuhlke.com", 1363424L); //Mike
        map.put("justin.cowling@zuhlke.com", 1363416L); //Justin
        map.put("brewster.barclay@zuhlke.com", 1363403L); //Brewster
        map.put("keith.braithwaite@zuhlke.com", 1363488L); //Keith
        map.put("peter.brown@zuhlke.com", 1415840L); //Peter Brown
        map.put("steve.freeman@zuhlke.com", 1415845L); //Steve Freeman
        map.put("john.seston@zuhlke.com", 1424149L); //John Seston
        map.put("sabine.streuss@zuhlke.com", 1424149L); //Sabine
        map.put("sabine.strauss@zuhlke.com", 1424149L); //Sabine
        map.put("ileana.meehan@zuhlke.com", 1424149L); //Ileana
        map.put("ina.hristova@zuhlke.com", 1424149L); //Ina
        map.put(null, 1277584L); //null

        this.teamIdMap = map;

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

    public Map<Long, Long> getOrgIdMap() {
        return orgIdMap;
    }

    public void setOrgIdMap(Map<Long, Long> orgIdMap) {
        this.orgIdMap = orgIdMap;
    }
}
