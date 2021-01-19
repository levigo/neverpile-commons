package com.neverpile.common.condition.json;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.EqualsCondition;
import com.neverpile.common.condition.ExistsCondition;
import com.neverpile.common.condition.OrCondition;
import com.neverpile.common.specifier.Specifier;

@RunWith(SpringRunner.class)
@JsonTest
public class ConditionJsonMarshallingTest {
  @Autowired
  ObjectMapper mapper;

  @Test
  public void testThat_unmarshallingSupportsNonSimpleForm() throws Exception {
    String json = "{" + //
        "  \"or\": {" + //
        "    \"conditions\": [" + //
        "      { \"equals\": { \"foo1\": true  } }," + //
        "      { \"equals\": { \"foo2\": true  } }" + //
        "    ]" + //
        "  }" + //
        "}";

    AndCondition and = (AndCondition) mapper.readValue(json, AndCondition.class);

    // and must contain a single or
    assertThat(and.getConditions()).hasSize(1);
    assertThat(and.getConditions().get(0)).isInstanceOf(OrCondition.class);

    // or must contain three equals
    OrCondition or = (OrCondition) and.getConditions().get(0);
    assertThat(or.getConditions()).hasSize(2);
    assertThat(or.getConditions()).allMatch(c -> c instanceof EqualsCondition);
    
    assertThat(((EqualsCondition)or.getConditions().get(0)).getPredicates()).containsEntry(Specifier.from("foo1"), Collections.singletonList("true"));
    assertThat(((EqualsCondition)or.getConditions().get(1)).getPredicates()).containsEntry(Specifier.from("foo2"), Collections.singletonList("true"));
  }
  
  @Test
  public void testThat_unmarshallingSupportsSimpleForm() throws Exception {
    String json = "{" + //
        "  \"or\": {" + //
        "    \"equals\": { \"foo1\": true  }," + 
        "    \"exists\": { \"target\": \"foo\" }" + 
        "  }" + //
        "}";
    
    AndCondition and = (AndCondition) mapper.readValue(json, AndCondition.class);
    
    // and must contain a single or
    assertThat(and.getConditions()).hasSize(1);
    assertThat(and.getConditions().get(0)).isInstanceOf(OrCondition.class);
    
    // or must contain three equals
    OrCondition or = (OrCondition) and.getConditions().get(0);
    assertThat(or.getConditions()).hasSize(2);

    assertThat(or.getConditions().get(0)).isInstanceOf(EqualsCondition.class);
    assertThat(((EqualsCondition)or.getConditions().get(0)).getPredicates()).containsEntry(Specifier.from("foo1"), Collections.singletonList("true"));
    
    assertThat(or.getConditions().get(1)).isInstanceOf(ExistsCondition.class);
    assertThat(((ExistsCondition)or.getConditions().get(1)).getTargets()).containsExactly("foo");
  }
  
  @Test
  public void testThat_marshallingSupportsSimpleForm() throws Exception {
    String json = "{" + //
        "  \"or\": {" + //
        "    \"equals\": { \"foo1\": [\"true\"]  }," + 
        "    \"exists\": { \"targets\": [\"foo\"] }" + 
        "  }" + //
        "}";
    
    AndCondition and = (AndCondition) mapper.readValue(json, AndCondition.class);
    
    String serialized = mapper.writeValueAsString(and);
    
    assertThat(serialized).isEqualToIgnoringWhitespace(json);
  }
  
  @Test
  public void testThat_marshallingSupportsNonSimpleForm() throws Exception {
    String json = "{" + //
        "  \"or\": {" + //
        "    \"conditions\": [" + //
        "      { \"equals\": { \"foo1\": [\"true\"]  } }," + //
        "      { \"equals\": { \"foo2\": [\"true\"]  } }" + //
        "    ]" + //
        "  }" + //
        "}";

    AndCondition and = (AndCondition) mapper.readValue(json, AndCondition.class);

    String serialized = mapper.writeValueAsString(and);
    
    assertThat(serialized).isEqualToIgnoringWhitespace(json);
  }

}
