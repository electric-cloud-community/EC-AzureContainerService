
use strict;
use warnings;
use ElectricCommander;
use Cwd qw(getcwd);
use Data::Dumper;

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
print $xpath->{_xml};

print Dumper $ec->getServerStatus();

my $path = getcwd() . "/EC-Kubernetes.jar";
print -f $path . "\n";

my $xpath = $ec->installPlugin($path);
print $xpath->{_xml};

$xpath = $ec->promotePlugin({pluginName => 'EC-Kubernetes-1.1.2.189'});
print $xpath->{_xml};
