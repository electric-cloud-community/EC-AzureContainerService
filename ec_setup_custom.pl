$commander->deleteArtifact({artifactName => 'com.electriccloud:@PLUGIN_KEY@-Grapes'});
if ($promoteAction eq 'promote') {
    $commander->createAclEntry({
        principalName => "project: $pluginName",
        principalType => "user",
        projectName => "/plugins/EC-Kubernetes/project",
        procedureName => "flowpdk-setup",
        executePrivilege => "allow",
        objectType => 'procedure',
    });
}
elsif ($promoteAction eq 'demote') {
    $commander->deleteAclEntry({
        principalName => "project: $pluginName",
        principalType => "user",
        projectName => "/plugins/EC-Kubernetes/project",
        procedureName => "flowpdk-setup",
        objectType => 'procedure',
    });
}
