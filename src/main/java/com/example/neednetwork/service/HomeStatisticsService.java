
package com.example.neednetwork.service;

import com.example.neednetwork.dto.HomeStatisticsResponse;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.ResourceRequestRepository;
import com.example.neednetwork.repository.UserRepository;

import org.springframework.stereotype.Service;

@Service
public class HomeStatisticsService {

    private final ResourceRequestRepository requestRepository;
    private final UserRepository userRepository;

    public HomeStatisticsService(
            ResourceRequestRepository requestRepository,
            UserRepository userRepository) {

        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    public HomeStatisticsResponse getStatistics() {

        // Count distinct resources successfully shared
        long resourcesShared =
                requestRepository.countDistinctSharedResources();

        // Count distinct people who received support
        long peopleSupported =
                requestRepository.countDistinctSupportedPeople();

        // Count active volunteers
        long volunteers =
                userRepository.countByRoleAndActiveTrue(
                        User.Role.VOLUNTEER
                );

        // Count active NGOs
        long ngos =
                userRepository.countByRoleAndActiveTrue(
                        User.Role.NGO
                );

        return new HomeStatisticsResponse(
                resourcesShared,
                peopleSupported,
                volunteers,
                ngos
        );
    }
}
