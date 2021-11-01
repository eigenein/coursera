package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * Provider class that builds the mean rating item scorer, computing item means from the
 * ratings in the DAO.
 */
public class ItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     * annotation on this parameter means that the DAO will be used to build the model, but the
     * model will <strong>not</strong> retain a reference to the DAO.  This is standard procedure
     * for LensKit models.
     */
    @Inject
    public ItemMeanModelProvider(@Transient final DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {
        final Long2DoubleOpenHashMap sums = new Long2DoubleOpenHashMap();
        final Long2DoubleOpenHashMap counts = new Long2DoubleOpenHashMap();

        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (final Rating rating : ratings) {
                counts.addTo(rating.getItemId(), 1.0);
                sums.addTo(rating.getItemId(), rating.getValue());
            }
        }

        final Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();
        for (final Map.Entry<Long, Double> entry : sums.entrySet()) {
            means.put((long) entry.getKey(), entry.getValue() / counts.get(entry.getKey()));
        }

        logger.info("computed mean ratings for {} items", means.size());
        return new ItemMeanModel(means);
    }
}
