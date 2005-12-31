/*
 * @(#)$Id$
 *
 * Copyright (c) 2005 Intel Corporation. All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE file.
 * If you do not find these files, copies can be found by writing to:
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300,
 * Berkeley, CA, 94704.  Attention:  Intel License Inquiry.
 * 
 * DESCRIPTION: P2's concrete type system: Int32 type.
 *
 */

#ifndef __VAL_INT32_H__
#define __VAL_INT32_H__

#include "value.h"
#include "oper.h"

class Val_Int32 : public Value {

public:  
  
  // Required fields for all concrete types.
  // The type name
  const Value::TypeCode typeCode() const { return Value::INT32; };
  const char *typeName() const { return "int32"; };
  virtual str toString() const { return strbuf() << i; };
  virtual unsigned int size() const { return sizeof(int32_t); }

  // Marshalling and unmarshallng
  void xdr_marshal_subtype( XDR *x );
  static ValuePtr xdr_unmarshal( XDR *x );

  // Constructor
  Val_Int32(int32_t theInt) : i(theInt) {};

  // Factory
  static ValuePtr mk(int32_t i) { ValuePtr p(new Val_Int32(i)); return p; };

  // Strict comparison
  int compareTo(ValuePtr) const;

  // Casting
  static int32_t cast(ValuePtr v);

  // The ZERO
  static ValuePtr ZERO;
  
  static const opr::Oper* oper_;
private:
  int32_t i;
  
};

#endif /* __VAL_INT32_H_ */
