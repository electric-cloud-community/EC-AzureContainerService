package com.electriccloud.commander.dsl.util

import groovy.io.FileType
import groovy.util.XmlSlurper
import java.io.File

import org.codehaus.groovy.control.CompilerConfiguration
import com.electriccloud.commander.dsl.DslDelegatingScript

abstract class BasePlugin extends DslDelegatingScript {

	def stepPicker (String pickerLabel, String pluginKey, String procedureName, String category, String description  = '') {

		// This is important, if the step-picker property does not have a description,
		// then the plugin lookup when defining procedure step fails with a GWT/JavaScript error.
		String propDescription = description ?: procedureName

		property "/server/ec_customEditors/pickerStep/$pickerLabel",
				value:
						"""<step>
						<project>/plugins/$pluginKey/project</project>
						<procedure>$procedureName</procedure>
						<category>$category</category>
					</step>
				""".stripIndent(),
				description: propDescription
	}

	def setupPluginMetadata(String pluginDir, String pluginKey, String pluginName, String pluginCategory) {
		getProcedures(pluginName).each { proc ->

			def addStepPicker = shouldAddStepPicker(pluginName, proc.procedureName)
			println "/projects/${pluginName}/procedures/${proc.procedureName}/standardStepPicker: '$addStepPicker'"
			if (addStepPicker) {
				def label = "$pluginKey - $proc.procedureName"
				def description = proc.description
				stepPicker (label, pluginKey, proc.procedureName, pluginCategory, description)
			}

		}
		// configure the plugin icon if is exists
		setPluginIconIfIconExists(pluginDir, pluginName)
	}

	def setPluginIconIfIconExists(String pluginDir, String pluginName) {

		String iconRelativePath = getPluginIcon(pluginDir)
		if (iconRelativePath) {
			println "Setting icon property /projects/${pluginName}/ec_icon to $iconRelativePath"
			property "/projects/${pluginName}/ec_icon", value: iconRelativePath
		}
	}

	def getPluginIcon(String pluginDir) {

		['svg', 'png'].findResult { ext ->
			File pluginIcon = new File("$pluginDir/htdocs/images", "icon-plugin.$ext")
			println "Checking icon file: $pluginIcon.absolutePath, exists? " + pluginIcon.exists()
			pluginIcon.exists() && pluginIcon.isFile() ?
					"images/icon-plugin.$ext" : null
		}
	}

	def cleanup(String pluginKey, String pluginName, String pluginCategory) {
		getProcedures(pluginName).each { proc ->

			def addStepPicker = shouldAddStepPicker(pluginName, proc.procedureName)
			// delete the step picker if it was added by setupCustomEditorData
			if (addStepPicker) {
				def label = "$pluginKey - $proc.procedureName"
				def propName = "/server/ec_customEditors/pickerStep/$label"
				def stepPickerProp = getProperty(propName, suppressNoSuchPropertyException: true)
				if (stepPickerProp) {
					deleteProperty propertyName: propName
				}
			}

		}
	}

	def shouldAddStepPicker(def pluginName, def procedureName) {
		if (procedureName == 'CreateConfiguration' || procedureName == 'DeleteConfiguration') {
			return false
		}
		def prop = getProperty("/projects/${pluginName}/procedures/${procedureName}/standardStepPicker", suppressNoSuchPropertyException: true)
		def value = prop?.value
		// If the property is not set, then we add the step-picker by default
		// If the property is set, then we check if the user requested stepPicker not be added.
		return value == null || (value != 'false' && value != '0')
	}

	def loadPluginProperties(String pluginDir, String pluginName) {

		// Recursively navigate each file or sub-directory in the properties directory
		//Create a property corresponding to a file,
		// or create a property sheet for a sub-directory before navigating into it
		loadNestedProperties("/projects/$pluginName", new File(pluginDir, 'dsl/properties'))
	}

	def loadNestedProperties(String propRoot, File propsDir) {

		propsDir.eachFile { dir ->
			int extension = dir.name.lastIndexOf('.')
			int endIndex = extension > -1 ? extension : dir.name.length()
			String propName = dir.name.substring(0, endIndex)
			String propPath = "${propRoot}/${propName}"
			if (dir.directory) {
				property propName, {
					loadNestedProperties(propPath, dir)
				}
			} else {
				def exists = getProperty(propPath, suppressNoSuchPropertyException: true, expand: false)
				if (exists) {
					modifyProperty propertyName: propPath, value: dir.text
				} else {
					createProperty propertyName: propPath, value: dir.text
				}

			}
		}
	}

	def loadProcedures(String pluginDir, String pluginKey, String pluginName, String pluginCategory) {

		// Loop over the sub-directories in the procedures directory
		// and evaluate procedures if a procedure.dsl file exists

		File procsDir = new File(pluginDir, 'dsl/procedures')
		procsDir.eachDir {

			File procDslFile = getProcedureDSLFile(it)
			if (procDslFile?.exists()) {
				println "Processing procedure DSL file ${procDslFile.absolutePath}"
				def proc = loadProcedure(pluginDir, pluginKey, pluginName, procDslFile.absolutePath)

				//create formal parameters using form.xml
				File formXml = new File(it, 'form.xml')
				if (formXml.exists()) {
					println "Processing form XML $formXml.absolutePath"
					buildFormalParametersFromFormXml(proc, formXml)
				}

			}

		}

		// plugin boiler-plate
		setupPluginMetadata(pluginDir, pluginKey, pluginName, pluginCategory)
	}

	def getProcedureDSLFile(File procedureDir) {

		if (procedureDir.name.toLowerCase().endsWith('_ignore')) {
			return null
		}

		File procDSLFile = new File(procedureDir, 'procedure.dsl')
		if(procDSLFile.exists()) {
			return procDSLFile
		} else {
			return new File(procedureDir, 'procedure.groovy')
		}
	}

	def loadProcedure(String pluginDir, String pluginKey, String pluginName, String dslFile) {
		return evalInlineDsl(dslFile, [pluginKey: pluginKey, pluginName: pluginName, pluginDir: pluginDir])
	}

	//Helper function to load another dsl script and evaluate it in-context
	def evalInlineDsl(String dslFile, Map bindingMap) {

		CompilerConfiguration cc = new CompilerConfiguration();
		cc.setScriptBaseClass(DelegatingScript.class.getName());
		GroovyShell sh = new GroovyShell(this.class.classLoader, bindingMap? new Binding(bindingMap) : new Binding(), cc);
		DelegatingScript script = (DelegatingScript)sh.parse(new File(dslFile))
		script.setDelegate(this);
		return script.run();
	}

	def buildFormalParametersFromFormXml(def proc, File formXml) {

		def formElements = new XmlSlurper().parseText(formXml.text)

		procedure proc.procedureName, {

			ec_parameterForm = formXml.text
			formElements.formElement.each { formElement ->
				formalParameter "$formElement.property",
						defaultValue: formElement.value,
						required: formElement.required,
						description: formElement.description,
						type: formElement.type,
						label: formElement.label

				if (formElement['attachedAsParameterToStep'] && formElement['attachedAsParameterToStep'] != '') {
					formElement['attachedAsParameterToStep'].toString().split(',').each { attachToStep ->
						println("Attaching parameter $formElement.property to step $attachToStep")
						attachParameter(projectName: proc.projectName,
								procedureName: proc.procedureName,
								stepName: attachToStep,
								formalParameterName: formElement.property)
					}
				}

				//setup custom editor data for each parameter
				property 'ec_customEditorData', procedureName: proc.procedureName, {
					property 'parameters', {
						property "$formElement.property", {
							formType = 'standard'
							println "Form element $formElement.property, type: '${formElement.type.toString()}'"
							if ('checkbox' == formElement.type.toString()) {
								checkedValue = formElement.checkedValue?:'true'
								uncheckedValue = formElement.uncheckedValue?:'false'
								initiallyChecked = formElement.initiallyChecked?:'0'
							} else if ('select' == formElement.type.toString() ||
									'radio' == formElement.type.toString()) {
								int count = 0
								property "options", {
									formElement.option.each { option ->
										count++
										property "option$count", {
											property 'text', value: "${option.name}"
											property 'value', value: "${option.value}"
										}
									}
									type = 'list'
									optionCount = count
								}
							}
						}
					}
				}

			}
		}
	}

	def upgrade(String upgradeAction, String pluginName,
				String otherPluginName, List steps,
				String configName = 'ec_plugin_cfgs') {

		migrationConfigurations(upgradeAction, pluginName, otherPluginName, steps, configName)
	}

	def migrationConfigurations(String upgradeAction, String pluginName,
								String otherPluginName, List steps,
								String configName = 'ec_plugin_cfgs') {

		if (upgradeAction == 'upgrade') {

			//Copy configurations from otherPluginName
			def configs = getProperty("/plugins/$otherPluginName/project/$configName", suppressNoSuchPropertyException: true)
			// bail if the other plugin does not have any configurations - nothing to do
			if (!configs) return

			//bail if the new plugin already has configurations
			def existingConfigs = getProperty("/plugins/$pluginName/project/$configName", suppressNoSuchPropertyException: true)
			if (existingConfigs) return

			clone path: "/plugins/$otherPluginName/project/$configName",
					cloneName: "/plugins/$pluginName/project/$configName"

			// Migrate 'privateKey' configuration parameter to a credential from all configurations
			def configurations = getProperties(path: "/plugins/$pluginName/project/$configName", recurse: true)
			configurations.property.each {
				def pluginConfig = it.propertyName
				println "Checking if configuration parameter 'privateKey' exists in plugin configuration '$pluginConfig'"

				// get the private key configuration property if it exists
				def oldPrivateKeyProperty = it.propertySheet?.property?.find { prop -> prop.name == 'privateKey'}
				def oldPrivateKey = oldPrivateKeyProperty?.value

				// if the property 'privateKey' exists, we assume it requires a migration to a crendential
				if (oldPrivateKeyProperty != null) {
					def keypairCredName = "${pluginConfig}_keypair"
					println "Converting configuration parameter 'privateKey' to a credential $keypairCredName"
					// delete the old property
					deleteProperty propertyName: "/plugins/$pluginName/project/$configName/$pluginConfig/privateKey"
					// create the new property for the keypair credential
					property "/plugins/$pluginName/project/$configName/$pluginConfig/keypair", value: keypairCredName

					createCredential(
							credentialName: keypairCredName,
							userName: 'key',
							password: oldPrivateKey,
							projectName: pluginName
					)
				}
			}

			def credentials = getCredentials("/plugins/$otherPluginName/project")
			if (credentials) {
				credentials.each { cred ->
					clone path: "/plugins/$otherPluginName/project/credentials/${cred.credentialName}",
							cloneName: "/plugins/$pluginName/project/credentials/${cred.credentialName}"

					aclEntry principalType: 'user',
							principalName: "project: $pluginName",
							projectName: pluginName,
							credentialName: cred.credentialName,
							objectType: 'credential',
							readPrivilege: 'allow',
							modifyPrivilege: 'allow',
							executePrivilege: 'allow',
							changePermissionsPrivilege: 'allow'

					steps.each { s ->
						attachCredential projectName: pluginName,
								credentialName: cred.credentialName,
								procedureName: s.procedureName,
								stepName: s.stepName
					}

				}
			}
		}
	}
}