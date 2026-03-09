package com.lagrodrigues.moviestore.listing;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for store listing persistence operations.
 */
@ApplicationScoped
public class StoreListingRepository implements PanacheRepository<StoreListing> {

    @Inject
    EntityManager entityManager;

    /**
     * Returns all listings belonging to the given merchant.
     *
     * @param merchantId the merchant identifier
     * @return the merchant's listings ordered by creation date descending
     */
    public List<StoreListing> findByMerchantId(UUID merchantId) {
        return find("merchant.id = ?1 order by createdAt desc", merchantId).list();
    }

    /**
     * Returns a listing by identifier if it belongs to the given merchant.
     *
     * @param listingId  the listing identifier
     * @param merchantId the merchant identifier
     * @return the matching listing, if found
     */
    public Optional<StoreListing> findByIdAndMerchantId(UUID listingId, UUID merchantId) {
        return find("id = ?1 and merchant.id = ?2", listingId, merchantId).firstResultOptional();
    }

    /**
     * Returns all active listings belonging to the given merchant.
     *
     * @param merchantId the merchant identifier
     * @return the merchant's active listings ordered by creation date descending
     */
    public List<StoreListing> findActiveByMerchantId(UUID merchantId) {
        return find("merchant.id = ?1 and active = true order by createdAt desc", merchantId).list();
    }

    /**
     * Returns an active listing by identifier if it belongs to the given merchant.
     *
     * @param listingId  the listing identifier
     * @param merchantId the merchant identifier
     * @return the matching active listing, if found
     */
    public Optional<StoreListing> findActiveByIdAndMerchantId(UUID listingId, UUID merchantId) {
        return find("id = ?1 and merchant.id = ?2 and active = true", listingId, merchantId)
                .firstResultOptional();
    }

    /**
     * Returns an active listing by identifier.
     *
     * @param listingId the listing identifier
     * @return the matching active listing, if found
     */
    public Optional<StoreListing> findActiveById(UUID listingId) {
        return find("id = ?1 and active = true", listingId).firstResultOptional();
    }

    /**
     * Returns a listing by identifier.
     *
     * @param listingId the listing identifier
     * @return the matching listing, if found
     */
    public Optional<StoreListing> findByIdOptional(UUID listingId) {
        return find("id", listingId).firstResultOptional();
    }

    /**
     * Returns a listing by identifier, applying a pessimistic write lock.
     *
     * @param listingId the listing identifier
     * @return the matching locked listing, if found
     */
    public Optional<StoreListing> findByIdForUpdate(UUID listingId) {
        return entityManager.createQuery(
                        "select s from StoreListing s where s.id = :listingId",
                        StoreListing.class
                )
                .setParameter("listingId", listingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}
