/* -*- C++ -*-
// -------------------------------------------------------------------
// MiniLisp - Very small lisp interpreter to demonstrate MiniExp.
// Copyright (c) 2005  Leon Bottou
//
// This software is subject to, and may be distributed under, the
// GNU General Public License, either Version 2 of the license,
// or (at your option) any later version. The license should have
// accompanied the software or you may obtain a copy of the license
// from the Free Software Foundation at http://www.fsf.org .
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// -------------------------------------------------------------------
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <ctype.h>
#include <math.h>

#include "miniexp.h"

#define CAT(a,b) __CAT(a,b)
#define __CAT(a,b) a ## b

miniexp_t s_quote = miniexp_symbol("quote");
miniexp_t s_true = miniexp_symbol("t");

/* ------------ error */

#ifdef __GNUC__
void error(const char *msg, miniexp_t v=0) __attribute__ ((noreturn));
#else
void error(const char *msg, miniexp_t v=0);
#endif

void
error(const char *msg, miniexp_t v)
{
  if (msg)
    printf("ERROR: %s", msg);
  else
    printf("BREAK");
  if (v)
    {
      printf(": ");
      miniexp_prin(v);
    }
  printf("\n");
  throw 0;
}



/* ------------ environment */

miniexp_t
lookup(miniexp_t var, miniexp_t env)
{
  while (miniexp_consp(env))
    {
      miniexp_t a = miniexp_car(env);
      if (miniexp_car(a) == var)
	return a;
      env = miniexp_cdr(env);
    }
  return 0;
}

minivar_t globalenv;

void
defvar(miniexp_t s, miniexp_t w = 0)
{
  minivar_t v;
  if (! globalenv)
    {
      minivar_t a = miniexp_cons(s_true, s_true);
      globalenv = miniexp_cons(a, 0);
    }
  if (! miniexp_symbolp(s))
    error("defvar: not a symbol", s);
  miniexp_t a = lookup(s, globalenv);
  if (a && w)
    {
      printf("WARNING: redefining '%s\n", miniexp_to_name(s));
      miniexp_rplacd(a, w);
    }
  else
    {
      v = miniexp_cons(s, w);
      v = miniexp_cons(v, miniexp_cdr(globalenv));
      miniexp_rplacd(globalenv, v);
    }
}


/* ------------ evaluate */

static bool break_request = false;

struct callable_t : public miniobj_t
{
  MINIOBJ_DECLARE(callable_t,miniobj_t,"callable");
  virtual miniexp_t call(miniexp_t args, miniexp_t env,
			 bool apply=false) = 0;
};

MINIOBJ_IMPLEMENT(callable_t,miniobj_t,"callable");

miniexp_t
evaluate(miniexp_t expr, miniexp_t env)
{
  if (miniexp_symbolp(expr))
    {
      miniexp_t a = lookup(expr,env);
      if (! a)
	error ("eval: undefined variable", expr);
      return miniexp_cdr(a);
    }
  else if (miniexp_consp(expr))
    {
      miniexp_t s = miniexp_car(expr);
      minivar_t xs = evaluate(s, env);
      miniobj_t *obj = miniexp_to_obj(xs);
      if (break_request)
	error(0);
      if (obj && obj->isa(callable_t::classname))
	return ((callable_t*)obj)->call(miniexp_cdr(expr), env);
      error("apply: cannot apply this object", xs);
    }
  else
    return expr;
}

miniexp_t
evaluate_progn(miniexp_t exprs, miniexp_t env)
{
  minivar_t v;
  while (miniexp_consp(exprs))
    {
      v = evaluate(miniexp_car(exprs),env);
      exprs = miniexp_cdr(exprs);
    }
  if (exprs)
    v = evaluate(exprs,env);
  return v;
}

miniexp_t
evaluate_list(miniexp_t l, miniexp_t env)
{
  minivar_t v;
  minivar_t ll = 0;
  miniexp_t lp = ll;
  if (miniexp_consp(l))
    {
      v = evaluate(miniexp_car(l), env);
      lp = ll = miniexp_cons(v, 0);
      l = miniexp_cdr(l);
    }
  while (miniexp_consp(l))
    {
      v = evaluate(miniexp_car(l), env);
      miniexp_rplacd(lp, miniexp_cons(v, 0));
      lp = miniexp_cdr(lp);
      l = miniexp_cdr(l);
    }
  if (l)
    {
      v = evaluate(l, env);
      if (lp)
	miniexp_rplacd(lp, v);
      else
	ll = v;
    }
  return ll;
}


/* ------------ special forms */

class specialform_t : public callable_t
{
  typedef miniexp_t (*fptr_t)(miniexp_t, miniexp_t);
  fptr_t fptr;
public:
  specialform_t(const char *name, fptr_t fptr);
  MINIOBJ_DECLARE(specialform_t,callable_t,"specialform");
  virtual miniexp_t call(miniexp_t args, miniexp_t env, bool);
};

MINIOBJ_IMPLEMENT(specialform_t,callable_t,"specialform");

specialform_t::specialform_t(const char *name, fptr_t fptr)
  : fptr(fptr)
{
  miniexp_t s = miniexp_symbol(name);
  minivar_t v = miniexp_object(this);
  defvar(s, v);
}

miniexp_t
specialform_t::call(miniexp_t args, miniexp_t env, bool)
{
  return (*fptr)(args, env);
}

#define DEFSPECIAL(s, n) \
miniexp_t CAT(f_,n)(miniexp_t, miniexp_t);\
specialform_t *CAT(p_,n) = new specialform_t(s, CAT(f_,n));\
miniexp_t CAT(f_,n)(miniexp_t expr, miniexp_t env)




/* ------------ primitives */

class primitive_t : public callable_t
{
  typedef miniexp_t (*fptr_t)(int, miniexp_t*, miniexp_t);
  fptr_t fptr;
  const int args;
  const int optargs;
public:
  primitive_t(const char *name, fptr_t fptr, int a, int o);
  MINIOBJ_DECLARE(primitive_t,callable_t,"primitive");
  virtual miniexp_t call(miniexp_t args, miniexp_t env, bool);
};

MINIOBJ_IMPLEMENT(primitive_t,callable_t,"primitive");

primitive_t::primitive_t(const char *n, fptr_t f, int a, int o)
  : fptr(f), args(a), optargs(o)
{
  miniexp_t s = miniexp_symbol(n);
  minivar_t v = miniexp_object(this);
  defvar(s, v);
}

miniexp_t
primitive_t::call(miniexp_t args, miniexp_t env, bool apply)
{
  int argc = miniexp_length(args);
  if (argc < this->args)
    error("apply(primitive): not enough arguments");
  if (argc > this->args + this->optargs)
    error("apply(primitive): too many arguments");
  minivar_t xargs = apply ? args : evaluate_list(args, env);
  miniexp_t *argv = new miniexp_t[argc];
  miniexp_t a = xargs;
  argc = 0;
  while (miniexp_consp(a))
    {
      argv[argc++] = miniexp_car(a);
      a = miniexp_cdr(a);
    }
  minivar_t v;
  try
    { v = (*fptr)(argc, argv, env); }
  catch(...)
    { delete [] argv; throw; }
  delete [] argv;
  return v;
}

#define DEFUN(s, n,a,o) \
miniexp_t CAT(f_,n)(int argc, miniexp_t *argv, miniexp_t env);\
primitive_t *CAT(p_,n) = new primitive_t(s, CAT(f_,n), a, o);\
miniexp_t CAT(f_,n)(int argc, miniexp_t *argv, miniexp_t env)


/* ------- functions */

class function_t : public callable_t
{
protected:
  miniexp_t args;
  miniexp_t body;
  miniexp_t env;
  static void check_args(miniexp_t a);
  static void match_args(miniexp_t a, miniexp_t v, miniexp_t &env);
public:
  function_t(miniexp_t, miniexp_t, miniexp_t);
  MINIOBJ_DECLARE(function_t,callable_t,"function");
  virtual miniexp_t call(miniexp_t args, miniexp_t env, bool);
  virtual void mark(minilisp_mark_t action);
  virtual miniexp_t funcdef(miniexp_t name=0);
};

MINIOBJ_IMPLEMENT(function_t,callable_t,"function");

void
function_t::check_args(miniexp_t a)
{
 again:
  if (miniexp_symbolp(a) || !a)
    return;
  if (miniexp_listp(a))
    {
      check_args(miniexp_car(a));
      a = miniexp_cdr(a);
      goto again;
    }
  error("lambda: illegal formal arguments");
}

void
function_t::match_args(miniexp_t a, miniexp_t v, miniexp_t &env)
{
 again:
  if (miniexp_symbolp(a))
    {
      minivar_t x = miniexp_cons(a,v);
      env = miniexp_cons(x, env);
      return;
    }
  if (miniexp_consp(a))
    {
      if (! miniexp_consp(v))
	error("apply: not enough arguments", a);
      match_args(miniexp_car(a), miniexp_car(v), env);
      a = miniexp_cdr(a);
      v = miniexp_cdr(v);
      goto again;
    }
  if (v)
    error("apply: too many arguments", v);
}

function_t::function_t(miniexp_t a, miniexp_t b, miniexp_t e)
  : args(a), body(b), env(e)
{
  check_args(a);
}

miniexp_t
function_t::call(miniexp_t args, miniexp_t env, bool apply)
{
  minivar_t xargs = apply ? args : evaluate_list(args, env);
  minivar_t nenv = this->env;
  match_args(this->args, xargs, nenv);
  return evaluate_progn(body, nenv);
}

void
function_t::mark(minilisp_mark_t action)
{
  action(&args);
  action(&body);
  action(&env);
}

miniexp_t
function_t::funcdef(miniexp_t name)
{
  if (name)
    {
      miniexp_t d = miniexp_symbol("defun");
      miniexp_t a = miniexp_cons(name, args);
      return miniexp_cons(d, miniexp_cons(a, body));
    }
  else
    {
      miniexp_t d = miniexp_symbol("lambda");
      return miniexp_cons(d,miniexp_cons(args,body));
    }
}


/* ------- macros */

class macrofunction_t : public function_t
{
public:
  macrofunction_t(miniexp_t a, miniexp_t b, miniexp_t e);
  MINIOBJ_DECLARE(macrofunction_t,function_t,"macrofunction");
  virtual miniexp_t call(miniexp_t args, miniexp_t env, bool);
  virtual miniexp_t funcdef(miniexp_t name=0);
};

MINIOBJ_IMPLEMENT(macrofunction_t,function_t,"macrofunction");

macrofunction_t::macrofunction_t(miniexp_t a, miniexp_t b, miniexp_t e)
  : function_t(a,b,e)
{
}

miniexp_t
macrofunction_t::call(miniexp_t args, miniexp_t env, bool)
{
  minivar_t nenv = this->env;
  match_args(this->args, args, nenv);
  minivar_t e = evaluate_progn(body, nenv);
  return evaluate(e, env);
}

miniexp_t
macrofunction_t::funcdef(miniexp_t name)
{
  if (name)
    {
      miniexp_t d = miniexp_symbol("defmacro");
      miniexp_t a = miniexp_cons(name, args);
      return miniexp_cons(d, miniexp_cons(a, body));
    }
  else
    {
      miniexp_t d = miniexp_symbol("mlambda");
      return miniexp_cons(d, miniexp_cons(args, body));
    }
}

/* ------------ define special forms */

DEFSPECIAL("progn",progn)
{
  return evaluate_progn(expr, env);
}

DEFSPECIAL("list",list)
{
  return evaluate_list(expr, env);
}

DEFSPECIAL("if",if)
{
  if (evaluate(miniexp_car(expr), env))
    return evaluate(miniexp_cadr(expr), env);
  return evaluate_progn(miniexp_cddr(expr), env);
}

DEFSPECIAL("setq",setq)
{
  if (miniexp_cddr(expr) || !miniexp_consp(miniexp_cdr(expr)))
    error("setq: syntax error");
  miniexp_t a = lookup(miniexp_car(expr),env);
  if (! a)
    error ("setq: undefined variable", miniexp_car(expr));
  minivar_t v = evaluate(miniexp_cadr(expr), env);
  miniexp_rplacd(a,v);
  return v;
}

DEFSPECIAL("defvar",defvar)
{
  if (miniexp_cddr(expr))
    error("defvar: syntax error");
  minivar_t v = evaluate(miniexp_cadr(expr), env);
  defvar(miniexp_car(expr), v);
  return miniexp_car(expr);
}

DEFSPECIAL("let",let)
{
  miniexp_t v = miniexp_car(expr);
  minivar_t nenv = env;
  minivar_t p, w;
  while (miniexp_consp(v))
    {
      miniexp_t a = miniexp_car(v);
      v = miniexp_cdr(v);
      if (! (miniexp_consp(a) &&
	     miniexp_symbolp(miniexp_car(a)) &&
	     !miniexp_cddr(a)))
	error("let: syntax error");
      w = evaluate(miniexp_cadr(a), env);
      p = miniexp_cons(miniexp_car(a), w);
      nenv = miniexp_cons(p, nenv);
    }
  return evaluate_progn(miniexp_cdr(expr), nenv);
}

DEFSPECIAL("letrec",letrec)
{
  miniexp_t v = miniexp_car(expr);
  minivar_t nenv = env;
  minivar_t p, w;
  while (miniexp_consp(v))
    {
      miniexp_t a = miniexp_car(v);
      v = miniexp_cdr(v);
      if (! (miniexp_consp(a) &&
	     miniexp_symbolp(miniexp_car(a)) &&
	     !miniexp_cddr(a)))
	error("let: syntax error");
      minivar_t p = miniexp_cons(miniexp_car(a), 0);
      nenv = miniexp_cons(p, nenv);
    }
  v = miniexp_car(expr);
  while (miniexp_consp(v))
    {
      miniexp_t a = miniexp_car(v);
      v = miniexp_cdr(v);
      w = evaluate(miniexp_cadr(a), nenv);
      p = lookup(miniexp_car(a), nenv);
      miniexp_rplacd(p,w);
    }
  return evaluate_progn(miniexp_cdr(expr), nenv);
}

DEFSPECIAL("lambda",lambda)
{
  miniexp_t args = miniexp_car(expr);
  miniexp_t body = miniexp_cdr(expr);
  function_t *f = new function_t(args, body, env);
  return miniexp_object(f);
}

DEFSPECIAL("mlambda",mlambda)
{
  miniexp_t args = miniexp_car(expr);
  miniexp_t body = miniexp_cdr(expr);
  function_t *f = new macrofunction_t(args, body, env);
  return miniexp_object(f);
}

DEFSPECIAL("quote",quote)
{
  if (miniexp_cdr(expr))
    error("quote: syntax error");
  return miniexp_car(expr);
}

DEFSPECIAL("while",while)
{
  if (! miniexp_consp(expr))
    error("while: syntax error");
  minivar_t v;
  while (evaluate(miniexp_car(expr), env))
    v = evaluate_progn(miniexp_cdr(expr), env);
  return v;
}

/* ------------ define primitive */

DEFUN("nullp",nullp,1,0) {
  return (!argv[0]) ? s_true : 0;
}

DEFUN("listp",listp,1,0) {
  return miniexp_listp(argv[0]) ? s_true : 0;
}

DEFUN("consp",consp,1,0) {
  return miniexp_consp(argv[0]) ? s_true : 0;
}

DEFUN("numberp",numberp,1,0) {
  return miniexp_numberp(argv[0]) ? s_true : 0;
}

DEFUN("doublep",doublep,1,0) {
  return miniexp_doublep(argv[0]) ? s_true : 0;
}

DEFUN("objectp",objectp,1,0) {
  return miniexp_objectp(argv[0]) ? s_true : 0;
}

DEFUN("symbolp",symbolp,1,0) {
  return miniexp_symbolp(argv[0]) ? s_true : 0;
}

DEFUN("stringp",stringp,1,0) {
  return miniexp_stringp(argv[0]) ? s_true : 0;
}

DEFUN("classof",classof,1,0) {
  return miniexp_classof(argv[0]);
}

DEFUN("car",car,1,0) {
  return miniexp_car(argv[0]);
}

DEFUN("cdr",cdr,1,0) {
  return miniexp_cdr(argv[0]);
}

DEFUN("caar",caar,1,0) {
  return miniexp_caar(argv[0]);
}

DEFUN("cadr",cadr,1,0) {
  return miniexp_cadr(argv[0]);
}

DEFUN("cdar",cdar,1,0) {
  return miniexp_cdar(argv[0]);
}

DEFUN("cddr",cddr,1,0) {
  return miniexp_cddr(argv[0]);
}

DEFUN("length",length,1,0) {
  return miniexp_number(miniexp_length(argv[0]));
}

DEFUN("reverse",reverse,1,0) {
  return miniexp_reverse(argv[0]);
}

DEFUN("cons",cons,2,0) {
  return miniexp_cons(argv[0],argv[1]);
}

DEFUN("nth",nth,2,0) {
  if (! miniexp_numberp(argv[0]))
    error("nth: integer number expected");
  return miniexp_nth(miniexp_to_int(argv[0]), argv[1]);
}

DEFUN("rplaca",rplaca,2,0) {
  return miniexp_rplaca(argv[0],argv[1]);
}

DEFUN("rplacd",rplacd,2,0) {
  return miniexp_rplacd(argv[0],argv[1]);
}

DEFUN("abs",abs,1,0) {
  return miniexp_double(fabs(miniexp_to_double(argv[0])));
}

DEFUN("+",plus,0,9999) {
  double s = 0;
  for (int i=0; i<argc; i++)
    {
      if (!miniexp_doublep(argv[i]))
	error("+: number expected");
      s += miniexp_to_double(argv[i]);
    }
  return miniexp_double(s);
}

DEFUN("*",times,0,9999) {
  double s = 1;
  for (int i=0; i<argc; i++)
    {
      if (!miniexp_doublep(argv[i]))
	error("*: number expected");
      s *= miniexp_to_double(argv[i]);
    }
  return miniexp_double(s);
}

DEFUN("-",minus,1,9999) {
  if (! miniexp_doublep(argv[0]))
    error("-: number expected");
  int i = 0;
  double s = 0;
  if (argc>1 && miniexp_doublep(argv[0]))
    s = miniexp_to_double(argv[i++]);
  while (i<argc && miniexp_doublep(argv[i]))
    s -= miniexp_to_double(argv[i++]);
  if (i < argc)
    error("-: number expected", argv[i]);
  return miniexp_double(s);
}

DEFUN("/",div,1,9999) {
  if (! miniexp_doublep(argv[0]))
    error("/: number expected");
  int i = 0;
  double s = 1;
  if (argc>1 && miniexp_doublep(argv[0]))
    s = miniexp_to_double(argv[i++]);
  while (i<argc && miniexp_doublep(argv[i]) && miniexp_to_double(argv[i]))
    s /= miniexp_to_double(argv[i++]);
  if (i < argc)
    {
      if (miniexp_doublep(argv[i]))
        error("/: division by zero", argv[i]);
      else
        error("/: number expected", argv[i]);
    }
  return miniexp_double(s);
}

DEFUN("==",equalequal,2,0) {
  return (argv[0]==argv[1]) ? s_true : 0;
}

static bool
equal(miniexp_t a, miniexp_t b)
{
  if (a == b)
    {
      return true;
    }
  else if (miniexp_consp(a) && miniexp_consp(b))
    {
      return equal(miniexp_car(a),miniexp_car(b))
        && equal(miniexp_cdr(a),miniexp_cdr(b));
    }
  else if (miniexp_doublep(a) && miniexp_doublep(b))
    {
      return miniexp_to_double(a) == miniexp_to_double(b);
    }
  else if (miniexp_stringp(a) && miniexp_stringp(b)) 
    {
      const char *sa, *sb;
      int la = miniexp_to_lstr(a, &sa);
      int lb = miniexp_to_lstr(b, &sb);
      return (la == lb) && ! memcmp(sa, sb, la);
    } 
  return false;
}

DEFUN("=",equal,2,0) {
  return equal(argv[0],argv[1]) ? s_true : 0;
}

DEFUN("<>",notequal,2,0) {
  return !equal(argv[0],argv[1]) ? s_true : 0;
}

static int
compare(miniexp_t a, miniexp_t b)
{
  if (miniexp_doublep(a) && miniexp_doublep(b))
    {
      double na = miniexp_to_double(a);
      double nb = miniexp_to_double(b);
      return (na < nb) ? -1 : (na > nb) ? +1 : 0;
    }
  else if (miniexp_stringp(a) && miniexp_stringp(b))
    {
      const char *sa, *sb;
      int la = miniexp_to_lstr(a, &sa);
      int lb = miniexp_to_lstr(b, &sb);
      int r = memcmp(sa, sb, (la < lb) ? la : lb);
      if (r == 0) 
        return (la < lb) ? -1 : (la > lb) ? +1 : 0;
      return r;
    }
  else
    error("compare: cannot rank these arguments");
}

DEFUN("<=",cmple,2,0) {
  return (compare(argv[0],argv[1])<=0) ? s_true : 0;
}

DEFUN("<",cmplt,2,0) {
  return (compare(argv[0],argv[1])<0) ? s_true : 0;
}

DEFUN(">=",cmpge,2,0) {
  return (compare(argv[0],argv[1])>=0) ? s_true : 0;
}

DEFUN(">",cmpgt,2,0) {
  return (compare(argv[0],argv[1])>0) ? s_true : 0;
}

DEFUN("floor",floor,1,0) {
  if (! miniexp_doublep(argv[0]))
    error("-: number expected");
  return miniexp_double(floor(miniexp_to_double(argv[0])));
}

DEFUN("ceil",ceil,1,0) {
  if (! miniexp_doublep(argv[0]))
    error("-: number expected");
  return miniexp_double(ceil(miniexp_to_double(argv[0])));
}

DEFUN("strlen",strlen,1,1) {
  if (! miniexp_stringp(argv[0]))
    error("strlen: string expected", argv[0]);
  return miniexp_number(miniexp_to_lstr(argv[0], 0));
}

DEFUN("substr",substr,2,1) {
  if (! miniexp_stringp(argv[0]))
    error("substr: string expected", argv[0]);
  const char *s;
  int l = miniexp_to_lstr(argv[0], &s);
  if (! miniexp_numberp(argv[1]))
    error("substr: integer number expected", argv[1]);
  int f = miniexp_to_double(argv[1]);
  f = (l < f) ? l : (f < 0) ? l : f;
  s += f;
  l -= f;
  if (argc>2)
    {
      if (! miniexp_numberp(argv[2]))
	error("substr: integer number expected", argv[2]);
      f = miniexp_to_double(argv[2]);
      l = (f > l) ? l : (f < 0) ? 0 : f;
    }
  return miniexp_lstring(l,s);
}

DEFUN("concat",concat,0,9999) {
  minivar_t l = 0;
  for (int i=0; i<argc; i++)
    if (miniexp_stringp(argv[i]))
      l = miniexp_cons(argv[i],l);
    else
      error("concat: string expected", argv[i]);
  l = miniexp_reverse(l);
  return miniexp_concat(l);
}

DEFUN("prin",prin,1,9999) {
  minivar_t v;
  v = miniexp_prin(argv[0]);
  for (int i=1; i<argc; i++)
    {
      minilisp_puts(" ");
      v = miniexp_prin(argv[i]);
    }
  return v;
}

DEFUN("print",print,1,9999) {
  minivar_t v;
  v = miniexp_prin(argv[0]);
  for (int i=1; i<argc; i++)
    {
      minilisp_puts(" ");
      v = miniexp_prin(argv[i]);
    }
  minilisp_puts("\n");
  return v;
}

DEFUN("pprint",pprint,1,1) {
  int w = 72;
  if (argc>1)
    {
      if (! miniexp_numberp(argv[1]))
	error("pprint: second argument must be number");
      w = miniexp_to_int(argv[1]);
    }
  return miniexp_pprint(argv[0], w);
}

DEFUN("pname",pname,1,1) {
  int w = 0;
  if (argc > 1)
    {
      if (! miniexp_numberp(argv[1]))
	error("pprint: second argument must be number");
      w = miniexp_to_int(argv[1]);
    }
  return miniexp_pname(argv[0],w);
}

DEFUN("gc",gc,0,0) {
  minilisp_gc();
  minilisp_info();
  return 0;
}

DEFUN("info",info,0,0) {
  minilisp_info();
  return 0;
}

DEFUN("funcdef",funcdef,1,1) {
  if (! miniexp_isa(argv[0], function_t::classname))
    error("funcdef: expecting function", argv[0]);
  if (argc>1 && ! miniexp_symbolp(argv[1]))
    error("funcdef: expecting symbol", argv[1]);
  function_t *f = (function_t*)miniexp_to_obj(argv[0]);
  return f->funcdef(argc>1 ? argv[1] : 0);
}

DEFUN("vardef",vardef,1,0) {
  miniexp_t a = lookup(argv[0],globalenv);
  if (! a)
    error("vardef: undefined global variable");
  return miniexp_cdr(a);
}

DEFUN("eval",eval,1,0) {
  return evaluate(argv[0],env);
}

DEFUN("apply",apply,2,0) {
  miniobj_t *obj = miniexp_to_obj(argv[0]);
  if (obj && obj->isa(callable_t::classname))
    return ((callable_t*)obj)->call(argv[1], env, true);
  error("apply: cannot apply this object", argv[0]);
}

DEFUN("error",error,1,1) {
  if (!miniexp_stringp(argv[0]))
    error("error: string expected", argv[0]);
  error(miniexp_to_str(argv[0]), (argc>1) ? argv[1] : 0);
}

DEFUN("display",display,0,9999) {
  for (int i=0; i<argc; i++)
    {
      minivar_t v = argv[i];
      if (! miniexp_stringp(v)) 
        v = miniexp_pname(v, 0);
      minilisp_puts(miniexp_to_str(v));
    }
  return 0;
}

DEFUN("string->symbol",string2symbol,1,0) {
  if (! miniexp_stringp(argv[0]))
    error("string->symbol: string expected",argv[0]);
  return miniexp_symbol(miniexp_to_str(argv[0]));
}

DEFUN("symbol->string",symbol2string,1,0) {
  if (! miniexp_symbolp(argv[0]))
    error("symbol->string: symbol expected",argv[0]);
  return miniexp_string(miniexp_to_name(argv[0]));
}

DEFUN("printflags",printflags,1,0) {
  if (! miniexp_numberp(argv[0]))
    error("printflags: integer number expected");
  minilisp_print_7bits = miniexp_to_int(argv[0]);
  return argv[0];
}

/* ------------ special */

#if defined(_WIN32) || defined(__WIN64)
# include <process.h>

class thread_t : public miniobj_t
{
  MINIOBJ_DECLARE(thread_t, miniobj_t, "thread");
private:
  uintptr_t thr;
  miniexp_t exp, env, res, run;
  static void start(void *arg) {
    thread_t *pth = (thread_t*) arg;
    try { 
      pth->res = evaluate(pth->exp, pth->env); 
      pth->run = miniexp_symbol("finished");
    } catch(...) { 
      pth->run = miniexp_symbol("error");
    } }
public:
  thread_t(miniexp_t exp, miniexp_t env) : exp(exp), env(env), res(0), run(0) { 
    thr = _beginthread(thread_t::start, 0, (void*)this); }
  void mark(minilisp_mark_t action) {
    action(&exp); action(&env), action(&res); }
  miniexp_t join() {
    return (run) ? res : miniexp_dummy; }
  miniexp_t status() { return run; }
  ~thread_t() { if (!run) abort(); join(); }
};

MINIOBJ_IMPLEMENT(thread_t, miniobj_t, "thread");

DEFUN("thread",threadstart,1,0) {
  return miniexp_object(new thread_t(argv[0],env));
}
DEFUN("threadp", threadtest,1,0) {
  if (! miniexp_isa(argv[0], thread_t::classname)) return 0;
  miniexp_t run = ((thread_t*)miniexp_to_obj(argv[0]))->status();
  return (run) ? run : miniexp_symbol("running");
}
DEFUN("join",threadjoin,1,0) {
  if (! miniexp_isa(argv[0], thread_t::classname))
    error("join: thread expected");
  return ((thread_t*)miniexp_to_obj(argv[0]))->join();
}
#endif

#ifdef HAVE_PTHREAD
# include <pthread.h>

class thread_t : public miniobj_t
{
  MINIOBJ_DECLARE(thread_t, miniobj_t, "thread");
private:
  pthread_t thr;
  miniexp_t exp, env, res, run;
  bool joined;
  static void* start(void *arg) {
    thread_t *pth = (thread_t*) arg;
    try { 
      pth->res = evaluate(pth->exp, pth->env); 
      pth->run = miniexp_symbol("finished");
      return 0; } 
    catch(...) { 
      pth->run = miniexp_symbol("error");
      return (void*)1; } }
public:
  thread_t(miniexp_t exp, miniexp_t env) 
    : exp(exp), env(env), res(0), run(0), joined(false) { 
    pthread_create(&this->thr, 0, thread_t::start, (void*)this); }
  void mark(minilisp_mark_t action) {
    action(&exp); action(&env), action(&res); }
  miniexp_t join() {
    if (! joined) pthread_join(thr, 0); joined=true;
    return (run) ? res : miniexp_dummy; }
  miniexp_t status() { return run; }
  ~thread_t() { if (!run) abort(); join(); }
};

MINIOBJ_IMPLEMENT(thread_t, miniobj_t, "thread");

DEFUN("thread",threadstart,1,0) {
  return miniexp_object(new thread_t(argv[0],env));
}
DEFUN("threadp", threadtest,1,0) {
  if (! miniexp_isa(argv[0], thread_t::classname)) return 0;
  miniexp_t run = ((thread_t*)miniexp_to_obj(argv[0]))->status();
  return (run) ? run : miniexp_symbol("running");
}
DEFUN("join",threadjoin,1,0) {
  if (! miniexp_isa(argv[0], thread_t::classname))
    error("join: thread expected");
  return ((thread_t*)miniexp_to_obj(argv[0]))->join();
}

#endif


/* ------------ toplevel */

void
toplevel(FILE *inp, FILE *out, bool print)
{
  miniexp_io_t saved_io = miniexp_io;
  minilisp_set_output(out);
  minilisp_set_input(inp);
  for(;;)
    {
      minivar_t s = miniexp_read();
      if (s == miniexp_dummy)
	{
          if (feof(inp)) break;
          printf("ERROR: while parsing\n");
	  break;
	}
      try
	{
	  break_request = false;
	  minivar_t v = evaluate(s, globalenv);
	  if (print)
	    {
	      printf("= ");
	      miniexp_print(v);
	    }
	}
      catch(...)
	{
	}
    }
  miniexp_io = saved_io;
}

miniexp_t
parse_comment(void)
{
  int c = minilisp_getc();
  while (c != EOF && c != '\n')
    c = minilisp_getc();
  return miniexp_nil;
}

miniexp_t
parse_quote(void)
{
  minivar_t l = miniexp_read();
  if (l == miniexp_dummy)
    return miniexp_dummy;
  l = miniexp_cons(s_quote, miniexp_cons(l, miniexp_nil));
  return miniexp_cons(l,miniexp_nil);
}

static void
sighandler(int signo)
{
  break_request = true;
  signal(signo, sighandler);
}

DEFUN("load",xload,1,0) {
  if (! miniexp_stringp(argv[0]))
    error("load: string expected");
  FILE *f = fopen(miniexp_to_str(argv[0]), "r");
  if (! f)
    error("load: cannot open file");
  toplevel(f, stdout, false);
  fclose(f);
  return miniexp_nil;
}


/* ------------ toplevel */

int
main()
{
#ifdef DEBUG
  minilisp_debug(1);
#endif
  minilisp_macrochar_parser[(int)';'] = parse_comment;
  minilisp_macrochar_parser[(int)'\''] = parse_quote;
  FILE *f = fopen("minilisp.in","r");
  if (f) {
    toplevel(f, stdout, false);
    fclose(f);
  } else
    printf("WARNING: cannot find 'minilisp.in'\n");
  signal(SIGINT, sighandler);
  while (! feof(stdin))
    toplevel(stdin, stdout, true);
  break_request = true;
  minilisp_finish();
  return 0;
}
