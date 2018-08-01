import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.*

class TestAcsClientTestMergingOfObjects extends Specification {

    def testMergeObj(def test) {
        def dest = new JsonSlurper().parseText(test.dest)
        def src = new JsonSlurper().parseText(test.src)
        def expectedResult = new JsonSlurper().parseText(test.expectedResult)
//        def expectedResultSrr = JsonOutput.prettyPrint(JsonOutput.toJson(expectedResult))

        AcsClient acsClient = new AcsClient()
        def result = acsClient.mergeObjs(dest, src)
//        def resultStr = JsonOutput.prettyPrint(JsonOutput.toJson(result))

        assert expectedResult == result: test.message

    }

    def testMergeObjResultIsEqualToDestination() {
        given:
        testMergeObj([
                message       : "result is equal to destination",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjNamespacePropertyFromDestinationIsKept() {
        given:
        testMergeObj([
                message       : "namespace property from destination is kept",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "THIS PROPERTY SHOULD NOT BE CHANGED",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "THIS PROPERTY SHOULD NOT BE CHANGED",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjPortIsOverrideByTheValueFromSource() {
        given:
        testMergeObj([
                message       : "port is overriden by the value from source",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 12345,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjObsoletePortsAreToBeRemoved() {
        given:
        testMergeObj([
                message       : "obsolete ports are to be removed",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            },
            {
                "name": "THIS TO BE REMOVED",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjNewPortsAreToBeAdded() {
        given:
        testMergeObj([
                message       : "new ports are to be added",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            },
            {
                "port": 12345,
                "name": "NEW PORT TO BE ADDED",
                "targetPort": "taget80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            },
            {
                "port": 12345,
                "name": "NEW PORT TO BE ADDED",
                "targetPort": "taget80",
                "protocol": "TCP"
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjPortsToBeReplacedDueToNoNameInDestination() {
        given:
        testMergeObj([
                message       : "ports to be fully replaced due to no 'name' attribute in one from destination",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "NAMEisAkey": "PORTS TO BE FULLY REPLACED",
                "protocol": "TCP",
                "port": 1234,
                "targetPort": "testtt",
                "nodePort": 1234423
            },
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "port": 810,
                "name": "servicehttpservice123nginx80",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    def testMergeObjPortsToBeReplacedDueToNoNameInSource() {
        given:
        testMergeObj([
                message       : "ports to be fully replaced due to no 'name' attribute in one from source",
                dest          : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "name": "servicehttpservice123nginx80",
                "protocol": "TCP",
                "port": 810,
                "targetPort": "null80",
                "nodePort": 32589
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
""",
                src           : """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "selector": {
            "ec-svc": "service123"
        },
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "ports": [
            {
                "port": 810,
                "NAMEisAkey": "THIS WILL OVERRIDE ALL PORTS IN DESTINATION",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ]
    }
}
""",
                expectedResult: """
{
    "kind": "Service",
    "apiVersion": "v1",
    "metadata": {
        "name": "service123",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/services/service123",
        "uid": "1b043142-8f35-11e8-87cf-000d3a4dabee",
        "resourceVersion": "1382574",
        "creationTimestamp": "2018-07-24T11:31:28Z",
        "labels": {
            "ec-svc-id": "f7c675a0-8f34-11e8-8eeb-0242ac1d0102"
        }
    },
    "spec": {
        "ports": [
            {
                "port": 810,
                "NAMEisAkey": "THIS WILL OVERRIDE ALL PORTS IN DESTINATION",
                "targetPort": "null80",
                "protocol": "TCP"
            }
        ],
        "selector": {
            "ec-svc": "service123"
        },
        "clusterIP": "10.0.152.231",
        "type": "LoadBalancer",
        "sessionAffinity": "None",
        "externalTrafficPolicy": "Cluster"
    },
    "status": {
        "loadBalancer": {
            "ingress": [
                {
                    "ip": "40.114.76.177"
                }
            ]
        }
    }
}
"""
        ])
    }

    class AcsClient {


        def mergeObjs(def dest, def src) {
            //Converting both object instances to a map structure
            //to ease merging the two data structures
            println("Destination to merge: " + JsonOutput.prettyPrint(JsonOutput.toJson(dest)))
            println("Source to merge: " + JsonOutput.prettyPrint(JsonOutput.toJson(src)))
            def result = mergeJSON((new JsonSlurper()).parseText((new JsonBuilder(dest)).toString()),
                    (new JsonSlurper()).parseText((new JsonBuilder(src)).toString()))
            println("After merge: " + JsonOutput.prettyPrint(JsonOutput.toJson(result)))
            return result
        }

        def mergeJSON(def dest, def src) {
            src.each { prop, value ->
                println("Has property $prop? value:" + dest[prop])
                if (dest[prop] != null && dest[prop] instanceof Map) {
                    mergeJSON(dest[prop], value)
                } else if (checkIfElementsAreListsAndSuitableForMerge(dest[prop], value)) {
                    mergeLists(dest[prop], value)
                } else {
                    dest[prop] = value
                }
            }
            return dest
        }

        boolean checkIfElementsAreListsAndSuitableForMerge(def dest, def src) {
            if (dest && src && dest instanceof List && src instanceof List
                    && checkIfAllElementsOfListAreMapsWithNameProperty(dest)
                    && checkIfAllElementsOfListAreMapsWithNameProperty(src)
                    && checkIfAllElementsOfListAreUniqueBasedOnNameKey(dest)
                    && checkIfAllElementsOfListAreUniqueBasedOnNameKey(src)
            ) {
                return true
            }
            return false
        }

        static boolean checkIfAllElementsOfListAreMapsWithNameProperty(def list) {
            list.every { it && it instanceof Map && it['name'] }
        }

        static boolean checkIfAllElementsOfListAreUniqueBasedOnNameKey(def list) {
            !list.countBy { it['name'] }.grep { it.value > 1 }
        }

        def mergeLists(def destList, def srcList) {
            // items which are not present in source list are to be removed from the destination list
            // items from source list which does not exists in destination list are to be added to the destination list
            def indicesOfUnmappedDestListItems = [] // to be removed
            def indicesOfUnmappedSrcListItems = [] // to be added
            def indicesOfMappedSrcListItems = []
            destList.eachWithIndex { destListItem, destListItemIndex ->
                def srcListItemIndex = srcList.findIndexOf { srcListItem ->
                    srcListItem['name'] == destListItem['name']
                }
                if (srcListItemIndex == -1) {
                    indicesOfUnmappedDestListItems.add(destListItemIndex)
                } else {
                    indicesOfMappedSrcListItems.add(srcListItemIndex)
                    destList[destListItemIndex] = mergeJSON(destList[destListItemIndex], srcList[srcListItemIndex])
                }
            }

            // remove unmapped destination list items from the destination list
            indicesOfUnmappedDestListItems.each { it -> destList.removeAt(it) }

            // add unmapped source list items to the destination list
            def allSrcListIndices = (0..srcList.size() - 1)
            indicesOfUnmappedSrcListItems = (allSrcListIndices - indicesOfMappedSrcListItems)
            indicesOfUnmappedSrcListItems.each { it -> destList.add(srcList[it]) }
        }
    }
}

//testMergeObjNamespacePropertyFromDestinationIsKept()
//testMergeObjNewPortsAreToBeAdded()
//testMergeObjObsoletePortsAreToBeRemoved()
//testMergeObjPortIsOverrideByTheValueFromSource()
//testMergeObjPortsToBeReplacedDueToNoNameInDestination()
//testMergeObjPortsToBeReplacedDueToNoNameInSource()
//testMergeObjResultIsEqualToDestination()
