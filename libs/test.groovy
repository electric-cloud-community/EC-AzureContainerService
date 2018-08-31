def projectName = 'acsProj'
def environmentName = 'acsEnv'
def clusterName = 'qe-acs-cluster'
def config = [
    publicKey: 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V',
    subscriptionId: '20ab6f85-801d-4c3a-a0d4-11da3631d29c',
    tenantId: '7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd'
]


//def privateKey = credentials[1].password //wait for fix
def privateKey = """-----BEGIN RSA PRIVATE KEY-----
MIIEpgIBAAKCAQEA5Tco0378+b64xU/6ft6s5QqJHOai5/fPaQDqS8CnucXx7doR
y40Hv7Crf/HqttRH4OSwNmoERqDsCa//8+NFhxyDGJkDr7WW+pDu1MuK/NWl+XT/
3WXDG3ulPxAE9FVT+1ZRE+Gft5kBb49Pw202tnTO0kP3eOkIcjkue4RyHROWvQ5w
1hffVoSNghifiV+hhVE9l58LEag6wMPm0+zbDHgcsM2tN6udFKArDH6sI6+x1UGl
/eQDRwlrJ+nAJFRL3oRo/Ql+s0MEGJEu36mbbd2DdkQRHvNXYmfpGteMoEkSQJMs
ILccOL1vpECdZ0MGNUD3MlKUzdMS1xCJcCsdlQIDAQABAoIBAQDOOfaNcu+Ac8wi
Gncxtn6wA+6hBhEDy73cp2olhcxiJGoqqczg6p5SJU3n62xV2mBhQuAX1QLL2PUF
MkiC0bQ4f7qjYugiKqsP/V4kLo16NW8DJfUe6PPFwlVGi88pVawdsed4j8L3mL3l
9Xa6SYS0XBvIqkX5jxB+VFwx3VQeO1E0ly07YHZRf23v4f2UxJBC2RKXlhahuo35
U9pxY6Q5Fuw4EbuMeSP+uFIrrTnQPeUTyMuJLdS4PHTjp4bnWZJA3TjVvsL7agxQ
yIGaiJVsfoiR8wO4xB31RBGtkTEtNvMJeOLOKIrkf/lZ/kSaw+JGPXgGoW/wJbqR
jyt1xHEBAoGBAP5zvaarMq3Tag8kEo4SYL25W5yAH7BNj1bV0x/nSFJ4p174EJcR
JXlp0SYwyn45NDV8ej69XRFza0mrn4K9zcte2U0PEovK2slXKMUHNwyShKCwAa8U
LDMd8/IenvR/eR6S+1Nkl7lqqJduGcdxaiw0nsZ9E6ug79RxjQxlW6jRAoGBAOac
HhbCnon8b5Mfjn5J9Hcnfh+5xHJDgqLqUyReR7y1Y3uKy7GE7OmTqF/kwSDZlgQc
bA7vR9OQrpbh/JDMp+h/C6Rq5rU/SZQp2LrUbSPOC20u/6dM/0acxTXCFTTrjHJz
sYfPnFVkvbD41Qc1E5SmEXcTpNLnkhQCB9WXEBmFAoGBANzWOOAhLz/b/+2aIhx5
6MpgyBjAvj/2YJQ9yhmjUop5OtHavBNQh1SjuSjLKcN5BE3zdelj0hVmNdRfhnYj
IywBatlNXoxDB7W81p6Bz44yz7biauO3v1CyC1m0p8KNq+IUMNpEsMX0wtOCdA87
6KjrFm4wedT4PY4E6yS6B14xAoGBAI+/uTHO1yyEYrh+7z/hsbWktIXqMn5XftM2
5eTBsE39c6nK9vYNOfDPziEYYxqrB0xAu0sD9O34Phxnehx4tiHDL+qpiG5qqN7m
xuaPDAlJou3IYixTXr0RdAzykm2OdtnRLFTQIU9JoMT0FzaK6025xyFG6XNeTjyS
l3eY1rxRAoGBAMUGSyk+6PHVc0u/5aw8pDaQsjVSLogjlFxJCbxClu5XFBQr+5yi
wrRYB0cav4hWk+AoaC8i5AX7jFew6HlHGrboK5f3xKUzZ/+hN5ZDE3WeTOUu6Jhy
hbqSMkv+5Q+ydfZW6uI8AfpyCL3NPU1Dztv/efwZK/NMKqC1ukM6LMYv
-----END RSA PRIVATE KEY-----"""


def credentials = [[
    userName: 'a90f65da-47a1-4547-aa41-6ca06cf2551a',
    password: 'SzqYyyQU1a+hJIYxJxWAAo15Br0oMTXUzvHd6qP/1qM='
],
[userName: 'SzqYyyQU1a+hJIYxJxWAAo15Br0oMTXUzvHd6qP/1qM=', password: privateKey]]
assert credentials.size() != null
def userName = credentials[0].userName
def password = credentials[0].password

import com.electriccloud.kubernetes.*

//def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def cluster = [clusterId: '',]
def clusterId = cluster.clusterId.toString()
def clusterParameters = Client.getClusterParametersMap(cluster)

def azAccessToken = Client.retrieveAccessToken(config.tenantId, userName, password)
println azAccessToken

//def masterFqdn = Client.getMasterFqdn(config.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
//def endpoint = "https://${masterFqdn}"
//def token = Client.retrieveOrchestratorAccessToken(config.publicKey,
//    clusterParameters.resourceGroupName,
//    clusterParameters.clusterName,
//    azAccessToken,
//    clusterParameters.adminUsername,
//    masterFqdn,
//    privateKey)
//
//assert endpoint
//assert token
//
//def client = new Client(endpoint, token)
//assert clusterId
//assert clusterName
//def clusterView = new ClusterView(kubeClient: client,
//    projectName: projectName,
//    environmentName: environmentName,
//    clusterName: clusterName,
//    clusterId: clusterId)
//def response
//
//response = clusterView.getRealtimeClusterTopology()


//response
