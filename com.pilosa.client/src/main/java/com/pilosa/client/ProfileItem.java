package com.pilosa.client;

import com.pilosa.client.exceptions.PilosaException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

public class ProfileItem {
    private int id;
    private Map<String, Object> attributes;

    ProfileItem() {
        this.attributes = new HashMap<>(0);
    }

    ProfileItem(int id, Map<String, Object> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    /**
     * Returns profile ID.
     *
     * @return profile ID
     */
    public int getID() {
        return this.id;
    }

    /**
     * Returns profile attributes.
     *
     * @return profile attributes
     */
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return String.format("ProfileItem(id=%d, attrs=%s)", this.id, this.attributes);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(31, 47)
                .append(this.id)
                .append(this.attributes)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProfileItem)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        ProfileItem rhs = (ProfileItem) obj;
        return new EqualsBuilder()
                .append(this.id, rhs.id)
                .append(this.attributes, rhs.attributes)
                .isEquals();
    }

    static ProfileItem fromMap(Map map) {
        Integer id = (Integer) map.get("id");
        if (id == null) {
            throw new PilosaException("Invalid profile, has no id key");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) map.get("attrs");
        if (attrs == null) {
            throw new PilosaException("Invalid profile, has no attributes key");
        }
        return new ProfileItem(id, attrs);
    }
}