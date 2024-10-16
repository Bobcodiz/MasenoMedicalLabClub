package red.stevo.code.masenomedlabclub.Entities.events;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class EventImages {

    @Id
    private String imageId;

    //private String eventId;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Events event;

}

