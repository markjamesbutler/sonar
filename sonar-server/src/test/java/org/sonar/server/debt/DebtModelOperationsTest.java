/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.debt;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelOperationsTest {

  @Mock
  CharacteristicDao dao;

  @Mock
  RuleDao ruleDao;

  @Mock
  MyBatis mybatis;

  @Mock
  SqlSession session;

  @Mock
  System2 system2;

  Date now = DateUtils.parseDate("2014-03-19");

  CharacteristicDto characteristicDto = new CharacteristicDto()
    .setId(1)
    .setKey("MEMORY_EFFICIENCY")
    .setName("Memory use")
    .setOrder(2)
    .setEnabled(true);

  CharacteristicDto subCharacteristicDto = new CharacteristicDto()
    .setId(2)
    .setKey("EFFICIENCY")
    .setName("Efficiency")
    .setParentId(1)
    .setEnabled(true);

  int currentId;

  DebtModelOperations service;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(now.getTime());

    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    currentId = 10;
    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        CharacteristicDto dto = (CharacteristicDto) args[0];
        dto.setId(++currentId);
        return null;
      }
    }).when(dao).insert(any(CharacteristicDto.class), any(SqlSession.class));

    when(mybatis.openSession()).thenReturn(session);
    service = new DebtModelOperations(mybatis, dao, ruleDao, system2);
  }

  @Test
  public void create_sub_characteristic() {
    when(dao.selectById(1, session)).thenReturn(characteristicDto);

    DebtCharacteristic result = service.create("Compilation name", 1);

    assertThat(result.id()).isEqualTo(currentId);
    assertThat(result.key()).isEqualTo("COMPILATION_NAME");
    assertThat(result.name()).isEqualTo("Compilation name");
    assertThat(result.parentId()).isEqualTo(1);
    assertThat(result.createdAt()).isEqualTo(now);
  }

  @Test
  public void fail_to_create_sub_characteristic_when_parent_id_is_not_a_root_characteristic() {
    when(dao.selectById(1, session)).thenReturn(subCharacteristicDto);

    try {
      service.create("Compilation", 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("A sub characteristic can not have a sub characteristic as parent.");
    }
  }

  @Test
  public void fail_to_create_sub_characteristic_when_parent_does_not_exists() {
    when(dao.selectById(1, session)).thenReturn(null);

    try {
      service.create("Compilation", 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Characteristic with id 1 does not exists.");
    }
  }

  @Test
  public void fail_to_create_sub_characteristic_when_name_already_used() {
    when(dao.selectByName("Compilation", session)).thenReturn(new CharacteristicDto());
    when(dao.selectById(1, session)).thenReturn(characteristicDto);

    try {
      service.create("Compilation", 1);
      fail();
    } catch (BadRequestException e) {
      assertThat(e.l10nKey()).isEqualTo("errors.is_already_used");
      assertThat(e.l10nParams().iterator().next()).isEqualTo("Compilation");
    }
  }

  @Test
  public void fail_to_create_sub_characteristic_when_wrong_permission() {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.DASHBOARD_SHARING);

    try {
      service.create("Compilation", 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
  }

  @Test
  public void create_characteristic() {
    when(dao.selectMaxCharacteristicOrder(session)).thenReturn(2);

    DebtCharacteristic result = service.create("Portability", null);

    assertThat(result.id()).isEqualTo(currentId);
    assertThat(result.key()).isEqualTo("PORTABILITY");
    assertThat(result.name()).isEqualTo("Portability");
    assertThat(result.order()).isEqualTo(3);
    assertThat(result.createdAt()).isEqualTo(now);
  }

  @Test
  public void create_first_characteristic() {
    when(dao.selectMaxCharacteristicOrder(session)).thenReturn(0);

    DebtCharacteristic result = service.create("Portability", null);

    assertThat(result.id()).isEqualTo(currentId);
    assertThat(result.key()).isEqualTo("PORTABILITY");
    assertThat(result.name()).isEqualTo("Portability");
    assertThat(result.order()).isEqualTo(1);
    assertThat(result.createdAt()).isEqualTo(now);
  }

  @Test
  public void rename_characteristic() {
    when(dao.selectById(10, session)).thenReturn(subCharacteristicDto);

    DebtCharacteristic result = service.rename(10, "New Efficiency");

    assertThat(result.key()).isEqualTo("EFFICIENCY");
    assertThat(result.name()).isEqualTo("New Efficiency");
    assertThat(result.updatedAt()).isEqualTo(now);
  }

  @Test
  public void not_rename_characteristic_when_renaming_with_same_name() {
    when(dao.selectById(10, session)).thenReturn(subCharacteristicDto);

    service.rename(10, "Efficiency");

    verify(dao, never()).update(any(CharacteristicDto.class), eq(session));
  }

  @Test
  public void fail_to_rename_unknown_characteristic() {
    when(dao.selectById(10, session)).thenReturn(null);

    try {
      service.rename(10, "New Efficiency");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("Characteristic with id 10 does not exists.");
    }
  }

  @Test
  public void move_up() {
    when(dao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2));
    when(dao.selectEnabledRootCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(2).setKey("PORTABILITY").setOrder(1),
      new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2)
    ));

    DebtCharacteristic result = service.moveUp(10);

    ArgumentCaptor<CharacteristicDto> argument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).update(argument.capture(), eq(session));

    assertThat(result.order()).isEqualTo(1);
    assertThat(argument.getAllValues().get(0).getOrder()).isEqualTo(2);
    assertThat(argument.getAllValues().get(0).getUpdatedAt()).isEqualTo(now);
    assertThat(argument.getAllValues().get(1).getOrder()).isEqualTo(1);
    assertThat(argument.getAllValues().get(1).getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void do_nothing_when_move_up_and_already_on_top() {
    when(dao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(1));
    when(dao.selectEnabledRootCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(1),
      new CharacteristicDto().setId(2).setKey("PORTABILITY").setOrder(2)
    ));

    service.moveUp(10);

    verify(dao, never()).update(any(CharacteristicDto.class), eq(session));
  }

  @Test
  public void move_down() {
    when(dao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2));
    when(dao.selectEnabledRootCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2),
      new CharacteristicDto().setId(2).setKey("PORTABILITY").setOrder(3)
    ));

    DebtCharacteristic result = service.moveDown(10);

    ArgumentCaptor<CharacteristicDto> argument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).update(argument.capture(), eq(session));

    assertThat(result.order()).isEqualTo(3);
    assertThat(argument.getAllValues().get(0).getOrder()).isEqualTo(2);
    assertThat(argument.getAllValues().get(0).getUpdatedAt()).isEqualTo(now);
    assertThat(argument.getAllValues().get(1).getOrder()).isEqualTo(3);
    assertThat(argument.getAllValues().get(1).getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void do_nothing_when_move_down_and_already_on_bottom() {
    when(dao.selectById(10, session)).thenReturn(new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2));
    when(dao.selectEnabledRootCharacteristics(session)).thenReturn(newArrayList(
      new CharacteristicDto().setId(2).setKey("PORTABILITY").setOrder(1),
      new CharacteristicDto().setId(10).setKey("MEMORY_EFFICIENCY").setOrder(2)
    ));

    service.moveDown(10);

    verify(dao, never()).update(any(CharacteristicDto.class), eq(session));
  }

  @Test
  public void delete_sub_characteristic() {
    BatchSession batchSession = mock(BatchSession.class);
    when(mybatis.openBatchSession()).thenReturn(batchSession);

    when(ruleDao.selectBySubCharacteristicId(2, batchSession)).thenReturn(newArrayList(
      new RuleDto()
        .setCharacteristicId(2).setRemediationFunction("LINEAR_OFFSET").setRemediationFactor("2h").setRemediationOffset("5min")
        .setDefaultCharacteristicId(10).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationFactor("4h").setDefaultRemediationOffset("15min")
    ));
    when(dao.selectById(2, batchSession)).thenReturn(subCharacteristicDto);

    service.delete(2);

    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(batchSession));
    RuleDto ruleDto = ruleArgument.getValue();

    // Overridden debt data are disabled
    assertThat(ruleDto.getCharacteristicId()).isEqualTo(-1);
    assertThat(ruleDto.getRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationFactor()).isNull();
    assertThat(ruleDto.getRemediationOffset()).isNull();
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(now);

    // Default debt data should not be touched
    assertThat(ruleDto.getDefaultCharacteristicId()).isEqualTo(10);
    assertThat(ruleDto.getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefaultRemediationFactor()).isEqualTo("4h");
    assertThat(ruleDto.getDefaultRemediationOffset()).isEqualTo("15min");

    ArgumentCaptor<CharacteristicDto> characteristicArgument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao).update(characteristicArgument.capture(), eq(batchSession));
    CharacteristicDto characteristicDto = characteristicArgument.getValue();

    // Sub characteristic is disable
    assertThat(characteristicDto.getId()).isEqualTo(2);
    assertThat(characteristicDto.isEnabled()).isFalse();
    assertThat(characteristicDto.getUpdatedAt()).isEqualTo(now);
  }

  @Test
  public void delete_characteristic() {
    BatchSession batchSession = mock(BatchSession.class);
    when(mybatis.openBatchSession()).thenReturn(batchSession);

    when(ruleDao.selectBySubCharacteristicId(subCharacteristicDto.getId(), batchSession)).thenReturn(newArrayList(
      new RuleDto().setCharacteristicId(subCharacteristicDto.getId()).setRemediationFunction("LINEAR_OFFSET").setRemediationFactor("2h").setRemediationOffset("5min")
    ));
    when(dao.selectCharacteristicsByParentId(1, batchSession)).thenReturn(newArrayList(
      subCharacteristicDto
    ));
    when(dao.selectById(1, batchSession)).thenReturn(characteristicDto);

    service.delete(1);

    verify(ruleDao).update(any(RuleDto.class), eq(batchSession));

    ArgumentCaptor<CharacteristicDto> characteristicArgument = ArgumentCaptor.forClass(CharacteristicDto.class);
    verify(dao, times(2)).update(characteristicArgument.capture(), eq(batchSession));
    CharacteristicDto subCharacteristicDto = characteristicArgument.getAllValues().get(0);
    CharacteristicDto characteristicDto = characteristicArgument.getAllValues().get(1);

    // Sub characteristic is disable
    assertThat(subCharacteristicDto.getId()).isEqualTo(2);
    assertThat(subCharacteristicDto.isEnabled()).isFalse();
    assertThat(subCharacteristicDto.getUpdatedAt()).isEqualTo(now);

    // Characteristic is disable
    assertThat(characteristicDto.getId()).isEqualTo(1);
    assertThat(characteristicDto.isEnabled()).isFalse();
    assertThat(characteristicDto.getUpdatedAt()).isEqualTo(now);
  }

}
