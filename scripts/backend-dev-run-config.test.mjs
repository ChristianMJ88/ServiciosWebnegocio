import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { JSDOM } from 'jsdom';

test('IDE backend runner uses dev configuration without overriding env credentials or UTC', async () => {
  const xml = await readFile('.run/Backend Spring Boot (dev).run.xml', 'utf8');
  const run = new JSDOM(xml, { contentType: 'text/xml' });
  const pom = new JSDOM(await readFile('backend/pom.xml', 'utf8'), { contentType: 'text/xml' });
  try {
    const doc = run.window.document;
    const option = name => doc.querySelector('MavenRunnerParameters > option[name="' + name + '"]');
    assert.deepEqual(option('cmdOptions').getAttribute('value').trim().split(/\s+/), ['-Dspring-boot.run.profiles=dev']);
    assert.deepEqual([...option('goals').querySelectorAll('option')].map(n => n.getAttribute('value')), ['spring-boot:run']);
    assert.equal(option('workingDirPath').getAttribute('value'), '$PROJECT_DIR$/backend');
    assert.equal(option('pomFileName').getAttribute('value'), 'pom.xml');
    assert.equal(doc.querySelector('MavenSettings > option[name="myRunnerSettings"]').children.length, 0);
    assert.doesNotMatch(xml, /-DDB_|-DJWT|jvmArguments/);
    const plugin = [...pom.window.document.querySelectorAll('build > plugins > plugin')]
      .find(n => n.querySelector('artifactId')?.textContent.trim() === 'spring-boot-maven-plugin');
    assert.equal(plugin.querySelector('configuration > jvmArguments').textContent.trim(), '-Duser.timezone=UTC');
  } finally { run.window.close(); pom.window.close(); }
});
