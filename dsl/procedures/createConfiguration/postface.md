### Make a remote connection to a Kubernetes, DC/OS, or Docker Swarm cluster
After creating an Azure Container Service cluster, you need to connect to the cluster to deploy and manage workloads. This article describes how to connect to the master VM of the cluster from a remote computer.

The Kubernetes, DC/OS, and Docker Swarm clusters provide HTTP endpoints locally. For Kubernetes, this endpoint is securely exposed on the internet, and you can access it by running the kubectl command-line tool from any internet-connected machine.

For DC/OS and Docker Swarm, we recommend that you create a secure shell (SSH) tunnel from your local computer to the cluster management system. After the tunnel is established, you can run commands which use the HTTP endpoints and view the orchestrator's web interface (if available) from your local system.

#### Prerequisites
 * A Kubernetes cluster deployed in Azure Container Service.
 * SSH RSA private key file, corresponding to the public key added to the cluster during deployment. These commands assume that the private SSH key is in $HOME/.ssh/id_rsa on your computer. See these instructions for macOS and Linux or Windows for more information. If the SSH connection isn't working, you may need to reset your SSH keys.
 
 ### Connect to a Kubernetes cluster
 Follow these steps to install and configure kubectl on your computer.
 
 ###### Note
 On Linux or macOS, you might need to run the commands in this section using sudo.
 
 ##### Install kubectl
 One way to install this tool is to use the az acs kubernetes install-cli Azure CLI 2.0 command. To run this command, make sure that you installed the latest Azure CLI 2.0 and logged in to an Azure account (az login).
 
 Linux or macOS
 * az acs kubernetes install-cli [--install-location=/some/directory/kubectl]
 
 Windows
 *  az acs kubernetes install-cli [--install-location=C:\some\directory\kubectl.exe]
 
 Alternatively, you can download the latest kubectl client directly from the Kubernetes releases page.
 
 ##### Download cluster credentials
 Once you have kubectl installed, you need to copy the cluster credentials to your machine. One way to do get the credentials is with the az acs kubernetes get-credentials command. Pass the name of the resource group and the name of the container service resource:
 
 Azure CLI
 
 * 'az acs kubernetes get-credentials --resource-group=(cluster-resource-group) --name=(cluster-name)'
 
 This command downloads the cluster credentials to $HOME/.kube/config, where kubectl expects it to be located.
 
 Alternatively, you can use scp to securely copy the file from $HOME/.kube/config on the master VM to your local machine. For example:
 
 bash
 
 * mkdir $HOME/.kube
 * scp azureuser@(master-dns-name):.kube/config $HOME/.kube/config
 
 If you are on Windows, you can use Bash on Ubuntu on Windows, the PuTTy secure file copy client, or a similar tool.
 
 ##### Use kubectl
 Once you have kubectl configured, test the connection by listing the nodes in your cluster:
 
 bash
 * kubectl get nodes
 
 You can try other kubectl commands. For example, you can view the Kubernetes Dashboard. First, run a proxy to the Kubernetes API server:
 
 bash
 * kubectl proxy
 
 The Kubernetes UI is now available at: localhost:8001/ui.
 
 For more information, see the Kubernetes quick start.
 
 