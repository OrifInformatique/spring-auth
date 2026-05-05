package ch.sectioninformatique.auth.app.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.transaction.Transactional;

@NoRepositoryBean
public interface SoftDeleteRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
    
    @Query(value="SELECT e FROM #{#entityName} e WHERE e.id = :id")
    Optional<T> findByIdWithDeleted(ID id);


    @Query(value="SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = true")
    Optional<T> findByIdDeleted(ID id);


    @Query(value="SELECT e FROM #{#entityName} e")
    List<T> findAllWithDeleted();


    @Query(value="SELECT e FROM #{#entityName} e WHERE e.deleted = true")
    List<T>findAllDeleted();


    @Transactional
    @Modifying
    @Query(value="UPDATE #{#entityName} e SET e.deleted = true WHERE e.id = :id")
    void softDeleteById(ID id);


    @Transactional
    @Modifying
    @Query(value="UPDATE #{#entityName} e SET e.deleted = false WHERE e.id = :id")
    void restoreById(ID id);


    @Transactional
    @Modifying
    @Query(value="DELETE FROM #{#entityName} e WHERE e.id = :id")
    void hardDeleteById(ID id);

}
