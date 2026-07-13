package kg.megalab.taskmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "department")
public class Department {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    /** Curating board member. */
    @ManyToOne
    private User curator;

    /** Head of the department. */
    @ManyToOne
    private User head;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getCurator() {
        return curator;
    }

    public void setCurator(User curator) {
        this.curator = curator;
    }

    public User getHead() {
        return head;
    }

    public void setHead(User head) {
        this.head = head;
    }
}
