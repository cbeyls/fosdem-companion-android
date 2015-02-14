package be.digitalia.fosdem.fragments;

import android.os.Bundle;

import java.util.ArrayList;

import be.digitalia.fosdem.adapters.SpeakerAdapter;
import be.digitalia.fosdem.model.Speaker;

/**
 * Created by Abhishek on 14/02/15.
 */
public class KeySpeakerFragment extends SmoothListFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<Speaker> speakerList = new ArrayList<Speaker>();
        speakerList.add(new Speaker("Colin Charles", "MariaDB Evangelist & VC", "", "", "http://fossasia.org/img/colin-charles.jpg", "Colin Charles is a businessman & into Open Source software. He is a consultant, technical architect, world traveller, and occasional writer. "));
        speakerList.add(new Speaker("Cat Allman", "Science Outreach and Open Source Program Manager at Google", "1", "1", "http://fossasia.org/img/catallman.jpg", "Cat Allman has been involved with the free and open source community since the mid 1980s, including stints at Mt Xinu, Sendmail, Inc, and the USENIX Association. Cat co-chaired the 1st Open Source Track at the 2010 Grace Hopper Celebration of Women. She is a co-organizer of the annual Science Foo Camp."));
        speakerList.add(new Speaker("Bunnie Huang", "Open Hardware Developer", "1", "1", "http://fossasia.org/img/bunniehuang.jpg", "Bunnie's passion for hardware began in elementary school. Since then he has garnered a PhD at MIT in EE, and has designed nanophotonic silicon chips, wireless radios and robotic submarines. He helps to create Novena, an open-source hardware laptop, and Chibitronics, unconventional electronics e.g. using paper with circuit stickers. "));
        speakerList.add(new Speaker("Lennart Poettering", "Developer of PulseAudio, Avahi, Systemd at RedHat", "dd", "", "http://fossasia.org/img/lennartpoettering.jpg", "Lennart grew up in Brazil and Germany. He is well-known as the initiator, developer and maintainer of software projects, which have been widely adopted in many Linux distributions."));
        SpeakerAdapter adapter = new SpeakerAdapter(getActivity().getApplicationContext(), speakerList);
        setListAdapter(adapter);
    }

}


