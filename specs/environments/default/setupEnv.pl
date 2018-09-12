use strict;
use warnings;
use ElectricCommander;


my $jobId = $ENV{COMMANDER_JOBID};
my $jobStepId = $ENV{COMMANDER_JOBSTEPID};

print "Job Step ID: $jobStepId\n";
for (keys %ENV) {
    if (/^COMMANDER/) {
        delete $ENV{$_};
    }
}

my $efServer = 'localhost';
my $ec = ElectricCommander->new({server => $efServer, debug => 1});
$ec->login('admin', 'changeme');
my $plugins = $ec->getPlugins();
my @dependencies = qw(EC-Kubernetes);
my @pluginNames = ();
for my $plugin ($plugins->findnodes('//plugin')) {
    my $name = $plugin->findvalue('pluginName');
    my $key = $plugin->findvalue('pluginKey');
    if (grep { $_ eq $key } @dependencies) {
        push @pluginNames, "$name";
        print "$name\n";
    }
}

for my $plugin (@pluginNames) {
    print "Promoting plugin $plugin\n";
    my $start = time;
    $ec->promotePlugin({pluginName => $plugin, promoted => 0});
    $ec->promotePlugin($plugin);
    my $end = time;
    my $eta = $end - $start;
    print "Done with plugin $plugin in $eta seconds\n";
}
