use Cwd;
use File::Spec;
use POSIX;
my $dir = getcwd;
my $logfile ="";
my $pluginDir;


if ( defined $ENV{QUERY_STRING} ) {    # Promotion through UI
    $pluginDir = $ENV{COMMANDER_PLUGINS} . "/$pluginName";
}
else {
    my $commanderPluginDir = $commander->getProperty('/server/settings/pluginsDirectory')->findvalue('//value');
    unless ( $commanderPluginDir && -d $commanderPluginDir ) {
        die "Cannot find commander plugin dir, please ensure that the option server/settings/pluginsDirectory is set up correctly";
    }
    $pluginDir = File::Spec->catfile($commanderPluginDir, $pluginName);
}

$logfile .= "Plugin directory is $pluginDir\n";

$commander->setProperty("/plugins/$pluginName/project/pluginDir", {value=>$pluginDir});
$logfile .= "Plugin Name: $pluginName\n";
$logfile .= "Current directory: $dir\n";

# Evaluate promote.groovy or demote.groovy based on whether plugin is being promoted or demoted ($promoteAction)
local $/ = undef;
# If env variable QUERY_STRING exists:
my $dslFilePath;
if(defined $ENV{QUERY_STRING}) { # Promotion through UI
    $dslFilePath = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, $pluginName, "dsl", "$promoteAction.groovy");
} else {  # Promotion from the command line
    $dslFilePath = File::Spec->catfile($pluginDir, "dsl", "$promoteAction.groovy");
}

$logfile .= "Evaluating dsl file: $dslFilePath\n";

open FILE, $dslFilePath or die "Couldn't open file: $dslFilePath: $!";
my $dsl = <FILE>;
close FILE;
my $dslReponse = $commander->evalDsl(
    $dsl, {
        parameters => qq(
                     {
                       "pluginName":"$pluginName",
                       "upgradeAction":"$upgradeAction",
                       "otherPluginName":"$otherPluginName"
                     }
              ),
        debug             => 'false',
        serverLibraryPath => File::Spec->catdir( $pluginDir, 'dsl' ),
    },
);


$logfile .= $dslReponse->findnodes_as_string("/");

my $errorMessage = $commander->getError();
if ( !$errorMessage ) {

    # Take copies of the file descriptors
    open OLDOUT, '>&STDOUT';
    open OLDERR, '>&STDERR';
    do {
        local *STDOUT;
        local *STDERR;

        my $device = $^O eq 'MSWin32' ? '>nul' : '>/dev/null';
        open(STDOUT, $device);
        open(STDERR, $device);
        print "OUT: Should Never happen\n";
        warn "ERR: Should Never happen\n";

    # This is here because we cannot do publishArtifactVersion in dsl today

    # delete artifact if it exists first
    $commander->deleteArtifactVersion("com.electriccloud:EC-AzureContainerService-Grapes:1.0.0");

    if ( $promoteAction eq "promote" ) {

        #publish jars to the repo server if the plugin project was created successfully
        my $am = new ElectricCommander::ArtifactManagement($commander);
        my $artifactVersion = $am->publish(
            {   groupId         => "com.electriccloud",
                artifactKey     => "EC-AzureContainerService-Grapes",
                version         => "1.0.0",
                includePatterns => "**",
                fromDirectory   => "$pluginDir/lib/grapes",
                description => "JARs that EC-AzureContainerService plugin procedures depend on"
            }
        );

        # Print out the xml of the published artifactVersion.
        $logfile .= $artifactVersion->xml() . "\n";

        if ( $artifactVersion->diagnostics() ) {
            $logfile .= "\nDetails:\n" . $artifactVersion->diagnostics();
        }
    }

    };

    # Restore stdout.
    open STDOUT, '>&OLDOUT' or die "Can't restore stdout: $!";
    open STDERR, '>&OLDERR' or die "Can't restore stderr: $!";
    close OLDOUT or die "Can't close OLDOUT: $!";
    close OLDERR or die "Can't close OLDERR: $!";
}

# Create output property for plugin setup debug logs
my $nowString = localtime;
$commander->setProperty( "/plugins/$pluginName/project/logs/$nowString", { value => $logfile } );

die $errorMessage unless !$errorMessage
