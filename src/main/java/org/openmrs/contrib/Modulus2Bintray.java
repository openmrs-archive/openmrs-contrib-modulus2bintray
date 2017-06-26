package org.openmrs.contrib;

import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Modulus to Bintray migration tool.
 *
 * Run command: mvn clean install exec:java -Dexec.args="bintrayUsername bintrayApiKey"
 */
public class Modulus2Bintray
{
    private static final HttpHost BINTRAY_HOST = new HttpHost("bintray.com", 443, "https");

    private static final String BINTRAY_REPO_URL = "https://bintray.com/api/v1/repos/openmrs/omod/";
    private static final String BINTRAY_PACKAGES_URL = "https://bintray.com/api/v1/packages/openmrs/omod/";
    private static final String BINTRAY_FILES_URL = "https://bintray.com/api/v1/content/openmrs/omod/";
    private static final String BINTRAY_FILE_METADATA_URL = "https://bintray.com/api/v1/file_metadata/openmrs/omod/";

    private static Executor bintrayExecutor;
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();


    public static void main(String[] args ) throws IOException, InterruptedException {
        bintrayExecutor = Executor.newInstance()
                .auth(BINTRAY_HOST, args[0], args[1]).authPreemptive(BINTRAY_HOST);

        Set<String> bintrayPackages = getBintrayPackages();

        Set<Module> modulusModules = getModulusModules();

        matchModulesWithBintrayPackages(modulusModules, bintrayPackages);

        System.out.println("\nIn Bintray and in Modulus:");
        int matched = 0;
        for (Module module: modulusModules) {
            if (module.getBintrayPackage() != null) {
                System.out.format("%-45s %-25s %-20s %n", module.getName(), module.getLegacyId(), module.getBintrayPackage());
                matched++;
            }
        }
        System.out.println("--- " + matched + " ---");


        System.out.println("\nIn Modulus, but not in Bintray:");
        int notMatched = 0;
        for (Module module: modulusModules) {
            if (module.getBintrayPackage() == null) {
                System.out.format("%-45s %-25s %n", module.getName(), module.getLegacyId());
                notMatched++;
            }
        }
        System.out.println("--- " + notMatched + " ---");

        BlockingQueue<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<>(20);
        ExecutorService executorService = new ThreadPoolExecutor(1, 50, 30,
                TimeUnit.SECONDS, linkedBlockingDeque,
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (Module module: modulusModules) {
            executorService.submit(() -> {
                try {
                    System.out.println("\nSyncing " + module.getName() + "...");
                    createBintrayPackageIfMissing(module);

                    updateBintrayPackage(module);

                    List<ModulusRelease> modulusReleases = getModulusReleases(module);

                    updateBintrayVersions(module, modulusReleases);

                    uploadMissingFiles(module, modulusReleases);
                    System.out.println("\nSynced " + module.getName() + "!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        //Need to do it as a post action since there's some lag between
        //uploading a file and being able to add it to download links
        for (Module module: modulusModules) {
            executorService.submit(() -> {
                try {
                    System.out.println("\nUpdating download links for " + module.getName() + "...");
                    updateDownloadLinks(module);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
    }

    private static void createBintrayPackageIfMissing(Module module) throws IOException {
        if (module.getBintrayPackage() == null) {
			BintrayPackage bintrayPackage = new BintrayPackage();
			if (module.getLegacyId() != null) {
				bintrayPackage.setName(module.getLegacyId());
			} else {
				String name = StringUtils.deleteWhitespace(module.getName().toLowerCase());
				bintrayPackage.setName(name);
			}
			bintrayPackage.setLicenses(Arrays.asList("MPL-2.0"));
			bintrayPackage.setVcs_url("https://github.com/openmrs");

			HttpResponse response = bintrayExecutor.execute(Request.Post(BINTRAY_PACKAGES_URL)
					.bodyString(gson.toJson(bintrayPackage), ContentType.APPLICATION_JSON)).returnResponse();
			failOnError(response);

			module.setBintrayPackage(bintrayPackage.getName());
		}
    }

    private static void updateDownloadLinks(Module module) throws IOException {
        Map<String, String> bintrayPackageFiles = getBintrayPackageFiles(module);
        for (Map.Entry<String, String> entry : bintrayPackageFiles.entrySet()) {
			String url = BINTRAY_FILE_METADATA_URL + entry.getValue();
			HttpResponse response = bintrayExecutor.execute(Request.Put(url).bodyString("{\"list_in_downloads\":true}", ContentType.APPLICATION_JSON)).returnResponse();
			failOnError(response);
		}
    }

    private static void updateBintrayVersions(Module module, List<ModulusRelease> modulusReleases) throws IOException {
        BintrayPackage bintrayPackage = getBintrayPackage(module);

        for (ModulusRelease modulusRelease: modulusReleases) {
            BintrayVersion bintrayVersion = new BintrayVersion(modulusRelease);

            if (bintrayPackage.getVersions().contains(modulusRelease.getModuleVersion())) {
                String url = BINTRAY_PACKAGES_URL + module.getBintrayPackage() + "/versions/" + modulusRelease.getModuleVersion();
                HttpResponse response = bintrayExecutor.execute(Request.Patch(url)
                        .bodyString(gson.toJson(bintrayVersion), ContentType.APPLICATION_JSON)).returnResponse();
                failOnError(response);
            } else {
                String url = BINTRAY_PACKAGES_URL + module.getBintrayPackage() + "/versions";
                HttpResponse response = bintrayExecutor.execute(Request.Post(url)
                        .bodyString(gson.toJson(bintrayVersion), ContentType.APPLICATION_JSON)).returnResponse();
                failOnError(response);
            }

            List<BintrayAttribute> attributes = new ArrayList<>();
            attributes.add(new BintrayAttribute("OpenMRS Core Compatibility", modulusRelease.getRequiredOMRSVersion()));
            attributes.add(new BintrayAttribute("Downloads (modulus)", modulusRelease.getDownloadCount().toString()));

            HttpResponse response = bintrayExecutor.execute(Request.Patch(BINTRAY_PACKAGES_URL + module.getBintrayPackage() + "/versions/"
                    + modulusRelease.getModuleVersion() + "/attributes")
                    .bodyString(gson.toJson(attributes), ContentType.APPLICATION_JSON))
                    .returnResponse();
            failOnError(response);
		}
    }

    private static void uploadMissingFiles(Module module, List<ModulusRelease> modulusReleases) throws IOException {
        Map<String, String> files = getBintrayPackageFiles(module);

        for (ModulusRelease modulusRelease: modulusReleases) {
			if (!files.containsKey(modulusRelease.getModuleVersion())) {
				URL downloadUrl = new URL(modulusRelease.getDownloadURL());
				try (InputStream inputStream = downloadUrl.openStream()) {
					String url = BINTRAY_FILES_URL + module.getBintrayPackage() + "/" + modulusRelease.getModuleVersion();
					url += String.format("/%s-%s.omod?publish=1&override=0", module.getBintrayPackage(), modulusRelease.getModuleVersion());

					HttpResponse response = bintrayExecutor.execute(Request.Put(url).bodyStream(inputStream)).returnResponse();
					failOnError(response);
				}
			}
		}
    }

    private static Map<String, String> getBintrayPackageFiles(Module module) throws IOException {
        Content response = bintrayExecutor.execute(Request.Get(BINTRAY_PACKAGES_URL + module.getBintrayPackage() + "/files?include_unpublished=1")).returnContent();
        List<BintrayFile> files = gson.fromJson(response.asString(), new TypeToken<List<BintrayFile>>() {
        }.getType());

        Map<String, String> versions = new HashMap<>();
        for (BintrayFile file: files) {
            versions.put(file.getVersion(), file.getPath());
        }
        return versions;
    }

    private static BintrayPackage getBintrayPackage(Module module) throws IOException {
        Content bintrayContent = bintrayExecutor.execute(Request.Get(BINTRAY_PACKAGES_URL + module.getBintrayPackage())).returnContent();
        return gson.fromJson(bintrayContent.asString(), new TypeToken<BintrayPackage>() {
		}.getType());
    }

    private static List<ModulusRelease> getModulusReleases(Module module) throws IOException {
        Content modulusContent = Request.Get("https://modules.openmrs.org/modulus/api/modules/" + module.getId() + "/releases?max=999&order=desc&sort=moduleVersion").execute().returnContent();
        return gson.fromJson(modulusContent.asString(), new TypeToken<List<ModulusRelease>>() {
		}.getType());
    }

    private static void failOnError(HttpResponse response) throws HttpResponseException {
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() >= 300) {
			throw new HttpResponseException(
					statusLine.getStatusCode(),
					statusLine.getReasonPhrase());
		}
    }

    private static void updateBintrayPackage(Module module) throws IOException {
        BintrayPackage bintrayPackage = new BintrayPackage();
        bintrayPackage.setWebsite_url(module.getDocumentationURL());
        bintrayPackage.setDesc(module.getDescription());
        String json = gson.toJson(bintrayPackage);
        HttpResponse response = bintrayExecutor.execute(Request.Patch(BINTRAY_PACKAGES_URL + module.getBintrayPackage())
				.bodyString(json, ContentType.APPLICATION_JSON)).returnResponse();
        failOnError(response);

        List<String> maintainers = new ArrayList<>();
        module.getMaintainers().forEach(maintainer -> {
            maintainers.add(maintainer.get("username"));
        });
        List<BintrayAttribute> attributes = new ArrayList<>();
        attributes.add(new BintrayAttribute("Maintainers", maintainers));
        response = bintrayExecutor.execute(Request.Patch(BINTRAY_PACKAGES_URL + module.getBintrayPackage() + "/attributes")
                .bodyString(gson.toJson(attributes), ContentType.APPLICATION_JSON)).returnResponse();
        failOnError(response);
    }

    private static void matchModulesWithBintrayPackages(Set<Module> modulusModules, Set<String> bintrayPackages) {
        bintrayPackages = new TreeSet<>(bintrayPackages);
        for (Module modulusModule: modulusModules) {
            if (modulusModule.getLegacyId() != null && bintrayPackages.remove(modulusModule.getLegacyId())) {
                modulusModule.setBintrayPackage(modulusModule.getLegacyId());
            } else if (modulusModule.getName() != null){
                String packageFromName = StringUtils.deleteWhitespace(modulusModule.getName().toLowerCase());
                if (bintrayPackages.remove(packageFromName)) {
                    modulusModule.setBintrayPackage(packageFromName);
                } else {
                    switch(modulusModule.getName()) {
                        case "Allergies UI":
                            if (bintrayPackages.remove("allergyui")) {
                                modulusModule.setBintrayPackage("allergyui");
                            }
                            break;
                        case "Allergies API":
                            if (bintrayPackages.remove("allergyapi")) {
                                modulusModule.setBintrayPackage("allergyapi");
                            }
                            break;
                        case "DHIS2 Reporting":
                            if (bintrayPackages.remove("dhisreport")) {
                                modulusModule.setBintrayPackage("dhisreport");
                            }
                            break;
                        case "OpenMRS FHIR":
                            if (bintrayPackages.remove("fhir")) {
                                modulusModule.setBintrayPackage("fhir");
                            }
                            break;
                        case "OpenHMIS Inventory":
                            if (bintrayPackages.remove("openhmis.inventory")) {
                                modulusModule.setBintrayPackage("openhmis.inventory");
                            }
                            break;
                        case "Open Web Apps":
                            if (bintrayPackages.remove("owa")) {
                                modulusModule.setBintrayPackage("owa");
                            }
                            break;
                        case "Paper Record Management":
                            if (bintrayPackages.remove("paperrecord")) {
                                modulusModule.setBintrayPackage("paperrecord");
                            }
                            break;
                    }
                }
            }
        }

        System.out.println("\nIn Bintray, but not in Modulus:");
        bintrayPackages.forEach(bintrayPackage -> System.out.println(bintrayPackage));
        System.out.println("--- " + bintrayPackages.size() + " ---");
    }

    private static Set<Module> getModulusModules() throws IOException {
        Set<Module> modulusModules = new TreeSet<>();
        int offset = 0;
        List<Module> modules;
        do {
            Content content = Request.Get("https://modules.openmrs.org/modulus/api/modules?sort=id&max=100&offset=" + offset)
                    .execute().returnContent();

            modules = gson.fromJson(content.asString(), new TypeToken<List<Module>>() {
            }.getType());

            modules.forEach(module -> {
                if (module.getName() != null) {
                    //Get rid of "Module" in module name
                    module.setName(module.getName().replace("Module", "").replace("module", "").trim());

                    if ("Reference Application".equals(module.getName()) && module.getLegacyId() == null) {
                        //skip broken entry
                    } else if ("Allergy UI".equals(module.getName()) && "htmlformentryui".equals(module.getLegacyId())) {
                        //skip broken entry
                    } else {
                        modulusModules.add(module);
                    }
                }
            });

            offset += 100;
        } while (!modules.isEmpty());


        return modulusModules;
    }

    private static Set<String> getBintrayPackages() throws IOException {
        int startPos = 0;
        List<Map<String, String>> pkgs;
        Set<String> bintrayPackages = new TreeSet<>();

        do {
            Content pkgsContent = bintrayExecutor.execute(Request.Get(BINTRAY_REPO_URL + "packages?start_pos=" + startPos))
                    .returnContent();
            pkgs = gson.fromJson(pkgsContent.asString(), new TypeToken<List<Map<String, String>>>() {}.getType());
            pkgs.forEach((pkg) -> bintrayPackages.add(pkg.get("name")));
            startPos += pkgs.size();
        } while (startPos % 100 == 0);

        return bintrayPackages;
    }
}
