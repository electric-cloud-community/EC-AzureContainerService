use strict;
use warnings;
use ElectricCommander;
use Cwd qw(getcwd);

my $jobId = $ENV{COMMANDER_JOBID};
my $jobStepId = $ENV{COMMANDER_JOBSTEPID};

my $chronic = ElectricCommander->new();

print "Job Step ID: $jobStepId\n";
for (keys %ENV) {
    if (/^COMMANDER/) {
        delete $ENV{$_};
    }
}

my $efServer = 'localhost';
my $ec = ElectricCommander->new({server => $efServer, debug => 1});
$ec->login('admin', 'changeme');

my $xpath = $chronic->retrieveArtifactVersions({
    artifactVersionName => 'com.electriccloud:EC-Kubernetes:1.1.2.189',
    toDirectory => getcwd(),
});
my $path = getcwd() . "/EC-Kubernetes.jar";
my $xpath = $ec->installPlugin($path);

$ec->promotePlugin({pluginName => 'EC-Kubernetes-1.1.2.189'});

