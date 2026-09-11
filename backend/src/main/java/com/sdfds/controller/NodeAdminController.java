package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.storage.model.NodeMetadata;
import com.sdfds.storage.node.StorageNode;
import com.sdfds.storage.registry.StorageNodeRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/nodes")
@RequiredArgsConstructor
@Tag(name = "Storage Node Administration", description = "Simulated storage node cluster management, health monitoring, and fault injection")
@SecurityRequirement(name = "Bearer Authentication")
public class NodeAdminController {

    private final StorageNodeRegistry nodeRegistry;

    @GetMapping
    @Operation(summary = "List storage node cluster", description = "Retrieves cluster status and capacity metrics for all storage nodes")
    public ResponseEntity<ApiResponse<List<NodeMetadata>>> getAllNodes() {
        List<NodeMetadata> nodes = nodeRegistry.getAllNodes().stream()
                .map(StorageNode::metadata)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(nodes, "Storage node cluster status retrieved successfully"));
    }

    @PostMapping("/{id}/simulate-failure")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Simulate storage node failure", description = "Manually trips a storage node status to DOWN to test failover & auto-healing")
    public ResponseEntity<ApiResponse<NodeMetadata>> simulateFailure(@PathVariable Long id) {
        nodeRegistry.updateNodeStatus(id, "DOWN");
        StorageNode node = nodeRegistry.getNode(id);
        return ResponseEntity.ok(ApiResponse.success(node.metadata(), "Storage node failure simulated (Status: DOWN)"));
    }

    @PostMapping("/{id}/recover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recover storage node", description = "Restores storage node status to HEALTHY")
    public ResponseEntity<ApiResponse<NodeMetadata>> recoverNode(@PathVariable Long id) {
        nodeRegistry.updateNodeStatus(id, "HEALTHY");
        StorageNode node = nodeRegistry.getNode(id);
        return ResponseEntity.ok(ApiResponse.success(node.metadata(), "Storage node recovered (Status: HEALTHY)"));
    }
}
