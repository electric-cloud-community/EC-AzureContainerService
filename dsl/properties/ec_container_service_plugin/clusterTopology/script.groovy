def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def config = args.configurationParameters

def credentials = args.credential
assert credentials.size() != null
def userName = credentials[0].userName
def password = credentials[0].password
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

import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import com.electriccloud.kubernetes.*

def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()
def clusterParameters = Client.getClusterParametersMap(cluster)

//try {
//    A.gogog()
//}
//catch (Exception e){
//    throw EcException
//            .code(ErrorCodes.ScriptError)
//            .message("t = ${clusterParameters.resourceGroupName}")
//            .cause(e)
//            .location(this.class.getCanonicalName())
//            .build()
//}
def azAccessToken = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyIsImtpZCI6IjdfWnVmMXR2a3dMeFlhSFMzcTZsVWpVWUlHdyJ9.eyJhdWQiOiJodHRwczovL21hbmFnZW1lbnQuYXp1cmUuY29tLyIsImlzcyI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0LzdiNGIxNGU1LTg3ZjgtNGYwOS05YzgzLWY5MWQ5YjhhNDlmZC8iLCJpYXQiOjE1MzI5Njc5NzIsIm5iZiI6MTUzMjk2Nzk3MiwiZXhwIjoxNTMyOTcxODcyLCJhaW8iOiI0MkJnWURqNjh1ejBzQ3dtZitHQ2hBZEN6MVdxQUE9PSIsImFwcGlkIjoiYTkwZjY1ZGEtNDdhMS00NTQ3LWFhNDEtNmNhMDZjZjI1NTFhIiwiYXBwaWRhY3IiOiIxIiwiZV9leHAiOjI2MjgwMCwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvN2I0YjE0ZTUtODdmOC00ZjA5LTljODMtZjkxZDliOGE0OWZkLyIsIm9pZCI6IjVhNDhkNTFhLWNhNzAtNGM5Yi04YTYzLTY2ZDNhMWU4NGFjZCIsInN1YiI6IjVhNDhkNTFhLWNhNzAtNGM5Yi04YTYzLTY2ZDNhMWU4NGFjZCIsInRpZCI6IjdiNGIxNGU1LTg3ZjgtNGYwOS05YzgzLWY5MWQ5YjhhNDlmZCIsInV0aSI6IkgteUV4SElzZFVpcWxTRmxCWFlqQUEiLCJ2ZXIiOiIxLjAifQ.EpsT9mcz0920OmbQCrmnFOls_Rqw167poh_wh_9EuQVUfthQXBxcv2K51V7Lw5P6X_SAPunfddgIG8hx_EnddbNrxFq1N4CBbQXkL-He0FQEGa2OA8cfZ6v-TP5mqDlGM4OnqjHdaHIIn-brOGCqd6GKhPA2lRYbC9iPbIPD8v6ze8y_-sVDnnak3kY_nGoq8FAjOrdTIC7wOhRv_nT0aNm-onVYadmwQ_GErfjldleHmMed_MGQBsrbZ8xdxkvodfnA2JTinepa2Bg-5C32-w7BanmVga8Pt18qmgS4LtVFvXRtXnXgUxlw_bIWNH_cZgPxMZc3JHcBgRDiLkOjkA'

//def azAccessToken = Client.retrieveAccessToken(config.tenantId, userName, password)
def masterFqdn = Client.getMasterFqdn(config.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
def endpoint = "https://${masterFqdn}"
def token = Client.retrieveOrchestratorAccessToken(config.publicKey,
        clusterParameters.resourceGroupName,
        clusterParameters.clusterName,
        azAccessToken,
        clusterParameters.adminUsername,
        masterFqdn,
        privateKey)



assert endpoint
assert token

def client = new Client(endpoint, token)
assert clusterId
assert clusterName
def clusterView = new ClusterView(kubeClient: client,
        projectName: projectName,
        environmentName: environmentName,
        clusterName: clusterName,
        clusterId: clusterId)
def response

try {
    response = clusterView.getRealtimeClusterTopology()
} catch (EcException e) {
    throw e
} catch (SocketTimeoutException | ConnectException e) {
    throw EcException
            .code(ErrorCodes.RealtimeClusterLookupFailed)
            .message("ACS API Endpoint ${endpoint} could not be reached - ${e.message}")
            .cause(e)
            .location(this.class.getCanonicalName())
            .build()
} catch (Throwable e) {
    throw EcException
            .code(ErrorCodes.ScriptError)
            .message("Exception occured while retrieving cluster topology: ${e.message}")
            .cause(e)
            .location(this.class.getCanonicalName())
            .build()
}

response