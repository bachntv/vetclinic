import { test } from 'node:test';
import assert from 'node:assert/strict';
import { can, canAccessPage, canAccessPath, pageRoles, roleProfiles, visitTransitions } from './permissions.ts';

test('cashier only has reception and payment pages, including direct URLs', () => {
  for (const page of ['dashboard','appointments','owners','pets','invoices','search','notifications','support']) assert.equal(canAccessPage('RECEPTIONIST',page),true,page);
  for (const page of ['users','audit','vets','medicines','vaccinations','vaccine-types','assistant','knowledge']) {
    assert.equal(canAccessPage('RECEPTIONIST',page),false,page);
    assert.equal(canAccessPath('RECEPTIONIST','/'+page),false,page);
  }
  assert.equal(canAccessPath('RECEPTIONIST','/invoices?visitId=42'),true);
  assert.equal(can('RECEPTIONIST','clinical.write'),false);
  assert.equal(can('RECEPTIONIST','catalog.manage'),false);
  assert.equal(can('RECEPTIONIST','owners.archive'),false);
});
test('doctor can treat patients and consult catalogs, without financial or administrative screens', () => {
  for (const page of ['appointments','pets','owners','vaccinations','medicines','vaccine-types','assistant','knowledge']) assert.equal(canAccessPage('VET',page),true,page);
  for (const path of ['/users','/audit','/vets','/invoices','/owners/new','/pets/12/edit']) assert.equal(canAccessPath('VET',path),false,path);
  assert.equal(canAccessPath('VET','/pets/12'),true);
  assert.equal(canAccessPath('VET','/visits/12'),true);
  assert.equal(can('VET','clinical.write'),true);
  assert.equal(can('VET','catalog.manage'),false);
  assert.equal(can('VET','invoices.manage'),false);
});
test('admin retains every screen and all operational actions', () => {
  for (const page of Object.keys(pageRoles)) assert.equal(canAccessPath('ADMIN','/'+page),true,page);
  for (const path of ['/owners/new','/pets/new','/owners/7/edit','/pets/8/edit']) assert.equal(canAccessPath('ADMIN',path),true,path);
  for (const action of ['patients.write','owners.archive','pets.archive','appointments.manage','clinical.write','catalog.manage','invoices.manage','support.manage']) assert.equal(can('ADMIN',action),true,action);
});
test('reception and clinical status actions are separated', () => {
  assert.deepEqual(visitTransitions('RECEPTIONIST','SCHEDULED'),['CHECKED_IN','CANCELLED']);
  assert.deepEqual(visitTransitions('RECEPTIONIST','CHECKED_IN'),['CANCELLED']);
  assert.deepEqual(visitTransitions('RECEPTIONIST','IN_EXAM'),[]);
  assert.deepEqual(visitTransitions('VET','SCHEDULED'),[]);
  assert.deepEqual(visitTransitions('VET','CHECKED_IN'),['IN_EXAM']);
  assert.deepEqual(visitTransitions('VET','IN_EXAM'),['COMPLETED']);
  for (const role of ['ADMIN','VET','RECEPTIONIST']) for (const state of ['COMPLETED','CANCELLED']) assert.deepEqual(visitTransitions(role,state),[]);
});
test('every menu and shortcut leads to a page accessible to that role', () => {
  for (const [role,profile] of Object.entries(roleProfiles)) {
    const links=[...profile.sections.flatMap(s=>s.items),...profile.shortcuts];
    for (const link of links) assert.equal(canAccessPath(role,link.to),true,role+': '+link.to);
  }
  assert.equal(roleProfiles.VET.sections.flatMap(s=>s.items).some(i=>i.page==='invoices'),false);
  assert.equal(roleProfiles.RECEPTIONIST.sections.flatMap(s=>s.items).some(i=>i.page==='medicines'),false);
});
test('login return URL cannot escape the workspace or bypass guarded child routes', () => {
  for (const path of ['https://example.com','//example.com','/pets/4/edit/extra','/pets/not-an-id','/invoices/unknown','/unknown','/visits/not-an-id']) assert.equal(canAccessPath('ADMIN',path),false,path);
  assert.equal(canAccessPath('RECEPTIONIST','/pets/7/edit?tab=basic'),true);
  assert.equal(canAccessPath('VET','/pets/7/edit?tab=basic'),false);
});
