package VPI;

import VPI.PDClasses.PDContactReceived;
import VPI.PDClasses.PDContactSend;
import VPI.VClasses.VContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sabu on 15/04/2016.
 */
public class Contacts {

    public List<VContact> vContacts;
    public List<PDContactReceived> pdContacts;

    public List<PDContactSend> postList;
    public List<PDContactSend> putList;

    public Contacts() {
        this.vContacts  = new ArrayList<>();
        this.pdContacts = new ArrayList<>();
        this.postList   = new ArrayList<>();
        this.putList    = new ArrayList<>();
    }
}
