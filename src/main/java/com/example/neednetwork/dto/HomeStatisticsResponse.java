
package com.example.neednetwork.dto;

public class HomeStatisticsResponse {

    private long resourcesShared;
    private long peopleSupported;
    private long communityVolunteers;
    private long partnerNgos;

    public HomeStatisticsResponse(
            long resourcesShared,
            long peopleSupported,
            long communityVolunteers,
            long partnerNgos) {

        this.resourcesShared = resourcesShared;
        this.peopleSupported = peopleSupported;
        this.communityVolunteers = communityVolunteers;
        this.partnerNgos = partnerNgos;
    }

    public long getResourcesShared() {
        return resourcesShared;
    }

    public long getPeopleSupported() {
        return peopleSupported;
    }

    public long getCommunityVolunteers() {
        return communityVolunteers;
    }

    public long getPartnerNgos() {
        return partnerNgos;
    }
}
