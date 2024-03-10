package org.pintoim.pinto.configuration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.pintoim.pinto.PintoServer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigLoaderSaver {
	private Config config;
	private File file;
	
	public ConfigLoaderSaver(Config config, File file) {
		this.config = config;
		this.file = file;
	}

	@SuppressWarnings("rawtypes")
	public void load() {
		try {
			if (!this.file.exists()) return;
			FileReader fileReader = new FileReader(this.file);
			Yaml yaml = new Yaml();
			Map configData = yaml.<Map>load(fileReader);
			fileReader.close();

			Field[] fields = this.config.getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!this.isFieldValid(field)) {
					continue;
				}
				
				try {
					Object newValue = field.get(this.config);
					
					if (configData.containsKey(field.getName())) {
						if (field.getType().isArray()) {
							ArrayList configValue = ((ArrayList)configData.get(field.getName()));
							Object configValueArray = Array.newInstance(field.getType().getComponentType(), 
									configValue.size());
							
						    for (int i = 0; i < configValue.size(); i++) {
						        Array.set(configValueArray, i, configValue.get(i));                     
						    }
						    
						    newValue = configValueArray;
						} else {
							newValue = configData.get(field.getName());	
						}
					}
					
					field.set(this.config, newValue);
				} catch (Exception ex) {
					PintoServer.logger.warn("Unable to load configuration field \"" + 
							field.getName() + "\": " + ex.getMessage());
				}
			}
		} catch (Exception ex) {
			PintoServer.logger.severe("Unable to load configuration: " +
					ex.getMessage());
		}
	}
	
	public void save() {
		try {
			try {
				if (!this.file.exists()) {
					this.file.createNewFile();
				}	
			} catch (Exception ex) {
				PintoServer.logger.severe("Unable to create configuration file \"" + 
						file.getName() + "\": " + ex.getMessage());
			}
			
			FileWriter fileWriter = new FileWriter(this.file);
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			Yaml yaml = new Yaml(dumperOptions);
			Map<String, Object> configData = new HashMap<String, Object>();
			
			Field[] fields = this.config.getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!this.isFieldValid(field)) {
					continue;
				}
				
				try {
					Object fieldValue = field.get(this.config);
					configData.put(field.getName(), fieldValue);
				} catch (Exception ex) {
					PintoServer.logger.warn("Unable to save configuration field \"" + 
							field.getName() + "\": " + ex.getMessage());
				}
			}

			yaml.dump(configData, fileWriter);
			fileWriter.flush();
			fileWriter.close();
		} catch (Exception ex) {
			PintoServer.logger.severe("Unable to save configuration: " +
					ex.getMessage());
		}
	}
	
	private boolean isFieldValid(Field field) {
		return !Modifier.isStatic(field.getModifiers()) &&
				!Modifier.isFinal(field.getModifiers()) &&
				!Modifier.isTransient(field.getModifiers());
	}
}
