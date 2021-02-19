package cz.devconf2021.lra.jpa;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * Data manipulation for {@link AdventurerTask} entity.
 */
@Dependent
@Transactional
public class AdventurerTaskRepository {
    @PersistenceContext
    private EntityManager em;

    public void save(AdventurerTask adventurerTask) {
        em.persist(adventurerTask);
    }

    public void update(AdventurerTask adventurerTask) {
        em.merge(adventurerTask);
    }

    public AdventurerTask get(int id) {
        return em.find(AdventurerTask.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<AdventurerTask> getAllTasks() {
        return em.createNamedQuery("AdventurerTask.findAll").getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<AdventurerTask> getByLraId(String lraId) {
        return em.createNamedQuery("AdventurerTask.findByLraId")
            .setParameter("lraId", lraId)
            .getResultList();
    }

    public List<AdventurerTask> getByType(TaskType type) {
        return em.createNamedQuery("AdventurerTask.findAllByType")
            .setParameter("type", type)
            .getResultList();
    }

    public AdventurerTask getFirstByLraId(String lraId) {
        List<AdventurerTask> adventurerTasks = getByLraId(lraId);
        if(adventurerTasks != null && !adventurerTasks.isEmpty()) return adventurerTasks.get(0);
        return null;
    }
}
